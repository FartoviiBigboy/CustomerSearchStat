package interfaces;

import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IResponseHandler<Entity> {
    public List<Entity> handleResponse(Connection connection, JSONObject parameter) throws SQLException;
}
