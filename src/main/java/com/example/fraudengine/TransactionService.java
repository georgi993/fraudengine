package com.example.fraudengine;

import com.example.fraudengine.datamodel.Transaction;
import com.example.fraudengine.redis.RedisTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RedisTransactionRepository redisTransactionRepository;

    private final String keyScammers = "scammers";
    private final String keyBlackListCountry = "blacklist_country";
    private final String keyFraudTransactions = "fraud_transactions";


    public String processTransactions(List<Transaction> transactions) throws SQLException {

        List<Transaction> fraudTransactions = new ArrayList<>();


        //Rule 3 - Suppose a transaction is created within the territory of a blacklisted country. You can assume that
        // you already have the blacklisted countries stored in the system database.
        for (Transaction transaction : transactions) {
            if (redisTransactionRepository.isMember(keyBlackListCountry, transaction.getCountry())) {
                fraudTransactions.add(transaction);
            }
        }


        //Rule 1 -  If a user has created more than 10 transactions within the last 1 min.
        //Rule 2 - If two user transactions are created from different places on Earth within the last 30 min and the
        // distance between those places is more than 300 kilometres.
        //Rule 4 - If a user has created transactions in 3 countries within the last 10 minutes.
        String transNumbers = hasDuplicateUsers(transactions, fraudTransactions);


        transactionRepository.saveTransactions(transactions);

        Set<String> userIdSet = fraudTransactions.stream()
                .map(transaction -> transaction.getUserId().toString())
                .collect(Collectors.toSet());

        Set<String> transIdSet = fraudTransactions.stream()
                .map(transaction -> transaction.getTransId().toString())
                .collect(Collectors.toSet());

        // if we have a new countries from the blackList
        Set<String> newBlacklistCountry = null;

        if (userIdSet.size() != 0) {
            // key - "scammers"
            redisTransactionRepository.saveFraud(keyScammers, userIdSet);
        }
        if (userIdSet.size() != 0) {
            // key - "fraud_transactions"
            redisTransactionRepository.saveFraud(keyFraudTransactions, transIdSet);
        }
        if (newBlacklistCountry != null) {
            // key - "blacklist_country"
            redisTransactionRepository.saveBlacklistCountry(keyBlackListCountry, newBlacklistCountry);
        }

        return transNumbers;
    }


    public String hasDuplicateUsers(List<Transaction> transactions, List<Transaction> fraudTransactions) {

        // Rule 1
        Map<Long/*userId*/, Map<Long/*counter*/, List<Transaction>/*transactions*/>> minuteFraudTransactions = new HashMap<>();
        // Rule 4
        Map<Long/*userId*/, Map<Long/*counter*/, List<Transaction>/*transactions*/>> tenMinutesFraudTransactions = new HashMap<>();
        // Rule 2
        Map<Long/*userId*/, Map<Long/*counter*/, List<Transaction>/*transactions*/>> halfHourFraudTransactions = new HashMap<>();

        long currentTimeMillis = System.currentTimeMillis();

        for (Transaction transaction : transactions) {

            TimeCondition condition = getTimeCondition(currentTimeMillis, transaction.getTimestamp().getTime());

            switch (condition) {
                case MINUTE:
                    if (!minuteFraudTransactions.containsKey(transaction.getUserId())) {
                        Map<Long, List<Transaction>> firstTransactions = new HashMap<>();
                        firstTransactions.put(1L, List.of(transaction));
                        minuteFraudTransactions.put(transaction.getUserId(), firstTransactions);
                    } else {
                        Map<Long, List<Transaction>> innerMap = minuteFraudTransactions.get(transaction.getUserId());
                        Map<Long, List<Transaction>> updatedMap = new HashMap<>();
                        List<Transaction> updatedList = new ArrayList<>();
                        updatedList.add(transaction);

                        for (Map.Entry<Long, List<Transaction>> entry : innerMap.entrySet()) {
                            List<Transaction> existingRecords = entry.getValue();
                            updatedList.addAll(existingRecords);
                            updatedMap.put(entry.getKey() + 1L, updatedList);
                        }
                        minuteFraudTransactions.put(transaction.getUserId(), updatedMap);
                    }
                    break;
                case TEN_MINUTES:
                    if (!tenMinutesFraudTransactions.containsKey(transaction.getUserId())) {
                        Map<Long, List<Transaction>> firstTransactions = new HashMap<>();
                        firstTransactions.put(1L, List.of(transaction));
                        tenMinutesFraudTransactions.put(transaction.getUserId(), firstTransactions);
                    } else {
                        Map<Long, List<Transaction>> innerMap = tenMinutesFraudTransactions.get(transaction.getUserId());
                        Map<Long, List<Transaction>> updatedMap = new HashMap<>();
                        List<Transaction> updatedList = new ArrayList<>();
                        updatedList.add(transaction);

                        for (Map.Entry<Long, List<Transaction>> entry : innerMap.entrySet()) {
                            List<Transaction> existingRecords = entry.getValue();
                            updatedList.addAll(existingRecords);
                            updatedMap.put(entry.getKey() + 1L, updatedList);
                        }
                        tenMinutesFraudTransactions.put(transaction.getUserId(), updatedMap);
                    }
                    break;
                case HALF_HOUR:
                    if (!halfHourFraudTransactions.containsKey(transaction.getUserId())) {
                        Map<Long, List<Transaction>> firstTransactions = new HashMap<>();
                        firstTransactions.put(1L, List.of(transaction));
                        halfHourFraudTransactions.put(transaction.getUserId(), firstTransactions);
                    } else {
                        Map<Long, List<Transaction>> innerMap = halfHourFraudTransactions.get(transaction.getUserId());
                        Map<Long, List<Transaction>> updatedMap = new HashMap<>();
                        List<Transaction> updatedList = new ArrayList<>();
                        updatedList.add(transaction);

                        for (Map.Entry<Long, List<Transaction>> entry : innerMap.entrySet()) {
                            List<Transaction> existingRecords = entry.getValue();
                            updatedList.addAll(existingRecords);
                            updatedMap.put(entry.getKey() + 1L, updatedList);
                        }
                        halfHourFraudTransactions.put(transaction.getUserId(), updatedMap);
                    }
                    break;
                case NONE:
                default:
                    break;
            }
        }

        // rule 1 - If a user has created more than 10 transactions within the last 1 min.
        for (Map.Entry<Long, Map<Long, List<Transaction>>> userEntry : minuteFraudTransactions.entrySet()) {
            Long userId = userEntry.getKey();
            Map<Long, List<Transaction>> countersMap = userEntry.getValue();

            for (Map.Entry<Long, List<Transaction>> counterEntry : countersMap.entrySet()) {
                Long counter = counterEntry.getKey();
                List<Transaction> minuteTransactions = counterEntry.getValue();
                if (counter > 10) {
                    fraudTransactions.addAll(minuteTransactions);
                }
                long uniqueCountriesCount = minuteTransactions.stream().map(Transaction::getCountry).distinct().count();
                if (uniqueCountriesCount == 3) {
                    fraudTransactions.addAll(minuteTransactions);
                }
            }
        }


        // rule 4 - If a user has created transactions in 3 countries within the last 10 minutes.
        for (Map.Entry<Long, Map<Long, List<Transaction>>> userEntry : tenMinutesFraudTransactions.entrySet()) {
            Long userId = userEntry.getKey();
            Map<Long, List<Transaction>> countersMap = userEntry.getValue();

            for (Map.Entry<Long, List<Transaction>> counterEntry : countersMap.entrySet()) {
                Long counter = counterEntry.getKey();
                List<Transaction> tenMinuteTransactions = counterEntry.getValue();

                long uniqueCountriesCount = tenMinuteTransactions.stream().map(Transaction::getCountry).distinct().count();
                if (uniqueCountriesCount == 3) {
                    fraudTransactions.addAll(tenMinuteTransactions);
                }
            }
        }


        // rule 2 - If two user transactions are created from different places on Earth within the last 30 min and the
        //distance between those places is more than 300 kilometres.
        for (Map.Entry<Long, Map<Long, List<Transaction>>> userEntry : halfHourFraudTransactions.entrySet()) {
            Long userId = userEntry.getKey();
            Map<Long, List<Transaction>> countersMap = userEntry.getValue();

            for (Map.Entry<Long, List<Transaction>> counterEntry : countersMap.entrySet()) {
                Long counter = counterEntry.getKey();
                List<Transaction> halfHourTransactions = counterEntry.getValue();


                long uniqueCountriesCount = halfHourTransactions.stream().map(Transaction::getCountry).distinct().count();
                if ((counter > 2) && (uniqueCountriesCount == 2)) {
                    Transaction transactionUK = halfHourTransactions.get(0);
                    Transaction transactionChina = halfHourTransactions.get(1);
                    Double distance = distanceManager(transactionUK.getLatitudeCoord(), transactionUK.getLongtudeCoord(), transactionChina.getLatitudeCoord(), transactionChina.getLongtudeCoord());

                    if (distance > 300) {
                        fraudTransactions.addAll(halfHourTransactions);
                    }
                }

            }
        }
        // return String with all not legitimate  transaction

        StringBuilder transIdsBuilder = new StringBuilder();

        for (Transaction transaction : fraudTransactions) {
            if (!transIdsBuilder.isEmpty()) {
                transIdsBuilder.append(", ");
            }
            transIdsBuilder.append(transaction.getTransId());
        }
        return transIdsBuilder.toString();
    }


    public Double distanceManager(Double latitudeCoordFirst, Double longtudeCoordFirst, Double latitudeCoordSecond, Double longtudeCoordSecond) {

        final int R = 6371; // Radius of the Earth in kilometers
        double dLat = Math.toRadians(latitudeCoordSecond - latitudeCoordFirst);
        double dLon = Math.toRadians(longtudeCoordSecond - longtudeCoordFirst);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitudeCoordFirst)) * Math.cos(Math.toRadians(latitudeCoordSecond)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    public void saveTransactions(List<Transaction> transactions) throws SQLException {
        transactionRepository.saveTransactions(transactions);
    }

    private TimeCondition getTimeCondition(long currentTime, long transactionTime) {
        long timeDifferenceInSeconds = (currentTime - transactionTime) / 1000;

        if (timeDifferenceInSeconds < 60) {
            return TimeCondition.MINUTE;
        } else if (timeDifferenceInSeconds < 600) {
            return TimeCondition.TEN_MINUTES;
        } else if (timeDifferenceInSeconds < 1800) {
            return TimeCondition.HALF_HOUR;
        } else {
            return TimeCondition.NONE;
        }
    }

    enum TimeCondition {
        MINUTE, TEN_MINUTES, HALF_HOUR, NONE
    }

}
