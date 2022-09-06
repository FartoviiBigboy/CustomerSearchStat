package interfaces;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public interface Repository<Entity> {
    List<Entity> read(PreparedStatement preparedStatement) throws SQLException;
}