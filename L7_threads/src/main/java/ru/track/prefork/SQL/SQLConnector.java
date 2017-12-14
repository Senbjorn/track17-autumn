package ru.track.prefork.SQL;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnector {

    public static Connection getConnection(String baseName) {
        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());

            StringBuilder url = new StringBuilder();

            url.
                    append("jdbc:mysql://").        //db type
                    append(baseName).    //host name tdb-1.trail5.net:
                    append("3306/").                //port
                    append("track17?").             //db name
                    append("user=track_student&").  //login
                    append("password=7EsH.H6x");    //password

            System.out.append("URL: " + url + "\n");
            Connection connection = DriverManager.getConnection(url.toString());
            return connection;
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void connect(String baseName) {
        Connection connection = getConnection(baseName);
        System.out.append("Connected!\n");
        try {
            System.out.append("Autocommit: " + connection.getAutoCommit() + '\n');
            System.out.append("DB name: " + connection.getMetaData().getDatabaseProductName() + '\n');
            System.out.append("DB version: " + connection.getMetaData().getDatabaseProductVersion() + '\n');
            System.out.append("Driver name: " + connection.getMetaData().getDriverName() + '\n');
            System.out.append("Driver version: " + connection.getMetaData().getDriverVersion() + '\n');
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
