package com.example.shortener;

import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres";
        Connection conn = DriverManager.getConnection(url);

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * from redirections");
        while (rs.next()) {
            System.out.print("Column 1 returned ");
            System.out.println(rs.getString(1));
        }
        rs.close();
        st.close();
    }
}
