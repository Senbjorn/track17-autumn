package ru.track.prefork.nioserver.myserver;

import ru.track.prefork.Message;
import ru.track.prefork.SQL.ConversationService;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySQLService implements ConversationService {

    MyShard shardOne;
    MyShard shardTwo;
    MyShard shardThree;


    public MySQLService(Connection one, Connection two, Connection three) throws SQLException {
        shardOne = new MyShard(one);
        shardTwo = new MyShard(two);;
        shardThree = new MyShard(three);;
    }

    @Override
    public long store(Message msg){
        MyShard base = getShardByMsg(msg);
        return base.insertMessage(msg);
    }

    @Override
    public List<Message> getHistory(long from, long to, long limit) {
        List<Message> allMessages = new ArrayList<>((int) (3 * limit));
        for (int i = 1; i <= 3; i++) {
            MyShard shard = getShardById(i);
            allMessages.addAll(shard.selectMessages(from, to, limit));
        }
        allMessages.sort((m1, m2)->{
            if (m1.getTime() > m2.getTime()) return 1;
            else if (m1.getTime() > m2.getTime()) return -1;
            return 0;
        });
        return allMessages.subList(0, (int) limit);
    }

    @Override
    public List<Message> getByUser(String username, long limit) {
        MyShard shard = getShardById(getShardIdByUsername(username));
        return shard.selectMessagesByUser(username, limit);
    }

    private MyShard getShardById(int id){
        switch (id) {
            case 0:
               return shardOne;
            case 1:
                return shardTwo;
            case 2:
                return shardThree;
            default:
                return null;
        }
    }

    private int getShardIdByUsername(String username) {
        char firsLetter = username.charAt(0);
        if (!Character.isLetter(firsLetter)) {
            return 0;
        }
        firsLetter = Character.toLowerCase(firsLetter);
        int value = firsLetter - 'a';
        return value/10;
    }

    private MyShard getShardByMsg(Message msg) {
        return getShardById(getShardIdByUsername(msg.getAuthor()));
    }

    public class MyShard {

        private Connection connection;
        private String insertMessages;
        private String selectMessages;
        private String selectMessagesByUser;

        public MyShard(Connection connection) throws SQLException {
            this.connection = connection;
            insertMessages = "INSERT INTO messages(user_name, text, ts) VALUES(?, ?, ?)";
            selectMessages = "SELECT * FROM messages WHERE ts > ? AND ts < ? ORDER BY ts DESC LIMIT ?";
            selectMessagesByUser = "SELECT * FROM messages WHERE user_name=? ORDER BY ts DESC LIMIT ?";
        }

        public long insertMessage(Message msg) {
            try {
                PreparedStatement insertMessages = connection.prepareStatement(this.insertMessages, Statement.RETURN_GENERATED_KEYS);
                insertMessages.setString(1, msg.getAuthor());
                insertMessages.setString(2, msg.getText());
                insertMessages.setTimestamp(3, new Timestamp(msg.getTime()));
                int affectedRows = insertMessages.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Insertion of message failed");
                }
                try (ResultSet generatedKeys = insertMessages.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Insertion of message failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                return -1;
            }
        }

        public List<Message> selectMessages(long from, long to, long limit){
            try {
                PreparedStatement selectMessages = connection.prepareStatement(this.selectMessages, Statement.RETURN_GENERATED_KEYS);
                selectMessages.setTimestamp(1, new Timestamp(from));
                selectMessages.setTimestamp(2, new Timestamp(to));
                selectMessages.setLong(3, limit);
                selectMessages.execute();
                List<Message> result = new ArrayList<>();
                try (ResultSet generatedKeys = selectMessages.getGeneratedKeys()) {
                    while (generatedKeys.next()) {
                        String author = generatedKeys.getString("user_name");
                        String text = generatedKeys.getString("text");
                        Timestamp ts = generatedKeys.getTimestamp("ts");
                        result.add(new Message(ts.getTime(), author, text));
                    }
                    return result;
                }
            } catch (SQLException e) {
                return Collections.emptyList();
            }
        }

        public List<Message> selectMessagesByUser(String username, long limit) {
            try {
                PreparedStatement selectMessagesByUser = connection.prepareStatement(this.selectMessagesByUser, Statement.RETURN_GENERATED_KEYS);
                selectMessagesByUser.setString(1, username);
                selectMessagesByUser.setLong(2, limit);
                selectMessagesByUser.execute();
                List<Message> result = new ArrayList<>((int) limit);
                try (ResultSet generatedKeys = selectMessagesByUser.getGeneratedKeys()) {
                    while (generatedKeys.next()) {
                        String author = generatedKeys.getString("user_name");
                        String text = generatedKeys.getString("text");
                        Timestamp ts = generatedKeys.getTimestamp("ts");
                        result.add(new Message(ts.getTime(), author, text));
                    }
                }
                return result;
            } catch (SQLException e) {
                return Collections.emptyList();
            }
        }

    }


    public static void main(String... args) throws SQLException {
        MySQLService mss = new MySQLService(null, null, null);
        System.out.println(mss.getShardIdByUsername("KJohn"));
    }
}
