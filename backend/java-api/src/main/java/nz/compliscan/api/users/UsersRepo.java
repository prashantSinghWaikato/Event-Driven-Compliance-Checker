package nz.compliscan.api.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;
import java.util.Optional;

@Repository
public class UsersRepo {

    private final DynamoDbEnhancedClient enhanced;
    private final DynamoDbTable<UserEntity> table;
    private final String emailIndex = "email-index";

    public UsersRepo(DynamoDbClient ddb,
            @Value("${app.aws.usersTable}") String tableName) {
        this.enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
        this.table = enhanced.table(tableName, TableSchema.fromBean(UserEntity.class));
    }

    public Optional<UserEntity> getByUsername(String username) {
        var key = Key.builder().partitionValue(username).build();
        var it = table.getItem(r -> r.key(key));
        return Optional.ofNullable(it);
    }

    public Optional<UserEntity> getByEmail(String email) {
        var index = table.index(emailIndex);
        var page = index.query(r -> r.queryConditional(QueryConditional.keyEqualTo(
                Key.builder().partitionValue(email).build()))).stream().findFirst();
        if (page.isEmpty())
            return Optional.empty();
        return page.get().items().stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        return getByUsername(username).isPresent();
    }

    public boolean existsByEmail(String email) {
        return getByEmail(email).isPresent();
    }

    public void put(UserEntity user) {
        table.putItem(user);
    }

    public void update(UserEntity user) {
        user.setUpdatedAt(Instant.now().toEpochMilli());
        table.updateItem(user);
    }
}
