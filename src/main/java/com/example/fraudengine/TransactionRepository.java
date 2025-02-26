package com.example.fraudengine;

import com.example.fraudengine.datamodel.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
@ConfigurationProperties
public class TransactionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String transactionQuery = "INSERT INTO transactions (trans_id, user_id, amount, timestamp, country, latitude_coord, longtude_coord) VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.url}")
    private String url;

    public void saveTransactions(List<Transaction> transactions) throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(url, username, password);
            preparedStatement = connection.prepareStatement(transactionQuery);

            for (Transaction transaction : transactions) {

                assert preparedStatement != null;

                preparedStatement.setLong(1, transaction.getTransId());
                preparedStatement.setLong(2, transaction.getUserId());
                preparedStatement.setBigDecimal(3, transaction.getAmount());
                preparedStatement.setTimestamp(4, transaction.getTimestamp());
                preparedStatement.setString(5, transaction.getCountry());
                preparedStatement.setDouble(6, transaction.getLatitudeCoord());
                preparedStatement.setDouble(7, transaction.getLongtudeCoord());
                preparedStatement.addBatch();
            }
            int[] result = preparedStatement.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert preparedStatement != null;
            preparedStatement.close();
            connection.close();
        }
    }
}
