package com.backend.orbitflow.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 여러 행 일괄 INSERT (JDBC 배치) : 행 수만큼 INSERT를 보내지 않도록
 *
 * <ul>
 *   <li>IDENTITY 키 엔티티는 JPA saveAll이 행마다 INSERT를 보내므로, 대량 저장 경로는 이 클래스를 사용</li>
 *   <li>데이터소스 URL의 rewriteBatchedStatements=true로 배치가 다중 행 INSERT 하나로 전송됨</li>
 *   <li>JPA와 같은 트랜잭션·커넥션을 사용하므로 같은 트랜잭션의 이후 조회에 바로 보이고, 롤백도 함께 됨</li>
 *   <li>엔티티 리스너·감사(created_at 등)를 거치지 않으므로 created_at·updated_at은 호출 측 값으로 채움</li>
 *   <li>생성된 id를 행 순서대로 반환</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JdbcBulkInserter {

    private final JdbcTemplate jdbcTemplate;

    // columns에 created_at·updated_at은 넣지 않음 (자동으로 now를 채움)
    public List<Long> insert(String table, List<String> columns, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String sql = "insert into " + table + " (" + String.join(", ", columns) + ", created_at, updated_at) values ("
                + "?, ".repeat(columns.size()) + "?, ?)";
        return jdbcTemplate.execute((ConnectionCallback<List<Long>>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (Object[] row : rows) {
                    for (int i = 0; i < row.length; i++) {
                        statement.setObject(i + 1, toJdbc(row[i]));
                    }
                    statement.setTimestamp(row.length + 1, now);
                    statement.setTimestamp(row.length + 2, now);
                    statement.addBatch();
                }
                statement.executeBatch();
                List<Long> ids = new ArrayList<>(rows.size());
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    while (keys.next()) {
                        ids.add(keys.getLong(1));
                    }
                }
                return ids;
            }
        });
    }

    private static Object toJdbc(Object value) {
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return value;
    }
}
