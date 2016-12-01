package george;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;



public class JdbcHelper
{
    // instance vars //////////////////////////////////////////////////////////
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private String errorMessage;    // current error message

    // for prepared statement
    private String activeSql;       // parametric SQL statement
    private PreparedStatement activeStatement;
    

    ///////////////////////////////////////////////////////////////////////////
    // ctor
    ///////////////////////////////////////////////////////////////////////////
    public JdbcHelper()
    {
        // init instance vars
        connection = null;
        statement = null;
        resultSet = null;
        errorMessage = "";
        
        activeSql = "";
        activeStatement = null;
    }



    ///////////////////////////////////////////////////////////////////////////
    // try to connect to DB with 3 params (URL, user, pass) and create static
    // statement object for statc SQL
    // if successful, return true. Otherwise, return false
    ///////////////////////////////////////////////////////////////////////////
    public boolean connect(String url, String user, String pass)
    {
        boolean connected = false;  // return value
        errorMessage = "";          // reset prev error message

        // validate input
        if(url == null || url.isEmpty())
        {
            errorMessage = "URL is null or empty in connect().";
            System.err.println(errorMessage);
            return connected;
        }
        if(user == null || user.isEmpty())
        {
            errorMessage = "Username in connect() cannot be null or empty.";
            System.err.println(errorMessage);
            return connected;
        }
        if(pass == null || pass.isEmpty())
        {
            errorMessage = "Password in connect() cannot be null or empty.";
            System.err.println(errorMessage);
            return connected;
        }

        try
        {
            // try to connect
            initJdbcDriver(url); // load jdbc driver first
            connection = DriverManager.getConnection(url, user, pass);

            // create statement object to store static SQL statements
            statement = connection.createStatement();

            connected = true;
        }
        catch(SQLException e)
        {
            connection = null;
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            connection = null;
            errorMessage = e.getMessage();
            System.err.println(errorMessage);    
        }
        return connected;
    }



    ///////////////////////////////////////////////////////////////////////////
    // clear JDBC recources
    // It simply ignores any error generated by close()
    ///////////////////////////////////////////////////////////////////////////
    public void disconnect()
    {
        activeSql = "";
        try{ resultSet.close(); }       catch(Exception e){}
        try{ statement.close(); }       catch(Exception e){}
        try{ activeStatement.close(); } catch(Exception e){} // for preparedstatement
        try{ connection.close(); }      catch(Exception e){}
    }



    ///////////////////////////////////////////////////////////////////////////
    // execute static SQL query statement
    // it ruturns ResultSet object if succesful. Otherwise, returns null.
    ///////////////////////////////////////////////////////////////////////////
    public ResultSet query(String sql)
    {
        // reset return value
        resultSet = null;
        errorMessage = "";

        // validate input
        if(sql == null || sql.isEmpty())
        {
            errorMessage = "SQL string in query() cannot be null or empty.";
            System.err.println(errorMessage);
            return resultSet;
        }

        try
        {
            // check connection first
            if(connection == null || connection.isClosed())
            {
                errorMessage = "Connection is NOT established yet. Connect to DB first before using query()";
                return resultSet;
            }

            resultSet = statement.executeQuery(sql);
        }
        catch(SQLException e)
        {
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            errorMessage = e.getMessage();
            System.err.println(errorMessage);
        }
        return resultSet;
    }



    ///////////////////////////////////////////////////////////////////////////
    // execute query for prepared statement
    ///////////////////////////////////////////////////////////////////////////
    public ResultSet query(String sql, String params)
    {
        // reset return value
        resultSet = null;
        errorMessage = "";

        // validate input
        if(sql == null || sql.isEmpty())
        {
            errorMessage = "SQL string in query() cannot be null or empty.";
            System.err.println(errorMessage);
            return resultSet;
        }

        try
        {
            // check connection first
            if(connection == null || connection.isClosed())
            {
                errorMessage = "Connection is NOT established yet. Connect to DB first before using query()";
                System.err.println(errorMessage);
                return resultSet;
            }

            // create new prepared statement only if SQL string was changed
            if(!activeSql.equals(sql))
            {
                activeSql = sql;
                activeStatement = connection.prepareStatement(sql);
            }

            // set all parameters for prepared statement
            setParametersForPreparedStatement(params);

            resultSet = activeStatement.executeQuery();
        }
        catch(SQLException e)
        {
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            errorMessage = e.getMessage();
            System.err.println(errorMessage);
        }
        return resultSet;
    }



    ///////////////////////////////////////////////////////////////////////////
    // execute update that returns no ResultSet, such as CREATE, UPDATE, etc.
    // if successful, it returns 0 or # of rows changed.
    // if failed, it returns -1
    ///////////////////////////////////////////////////////////////////////////
    public int update(String sql)
    {
        // reset return value
        int result = -1; // default return value
        errorMessage = "";

        // validate input
        if(sql == null || sql.isEmpty())
        {
            errorMessage = "SQL string in update() cannot be null or empty.";
            System.err.println(errorMessage);
            return result;
        }

        try
        {
            // check connection first
            if(connection == null || connection.isClosed())
            {
                errorMessage = "Database is NOT connected. Connect to DB before using update().";
                System.err.println(errorMessage);
                return result;
            }

            result = statement.executeUpdate(sql);
        }
        catch(SQLException e)
        {
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            errorMessage = e.getMessage();
            System.err.println(errorMessage);
        }

        // return result of executeUpdate()
        return result;
    }



    ///////////////////////////////////////////////////////////////////////////
    // execute update for preparedstatement
    // if successful, it returns 0 or # of rows changed.
    // if failed, it returns -1
    ///////////////////////////////////////////////////////////////////////////
    public int update(String sql, String params)
    {
        // reset return value
        int result = -1; // default return value
        errorMessage = "";

        // validate input
        if(sql == null || sql.isEmpty())
        {
            errorMessage = "SQL string in update() cannot be null or empty.";
            System.err.println(errorMessage);
            return result;
        }

        try
        {
            // check connection first
            if(connection == null || connection.isClosed())
            {
                errorMessage = "Database is NOT connected. Connect to DB before using update().";
                System.err.println(errorMessage);
                return result;
            }

            // compare sql string with the prev sql
            // if it is changed, create new prepared statement
            if(!activeSql.equals(sql))
            {
                activeStatement = connection.prepareStatement(sql);
                activeSql = sql; // remember the current sql
            }

            // set params for prepared statement
            setParametersForPreparedStatement(params);

            // finally execute sql
            result = activeStatement.executeUpdate();
        }
        catch(SQLException e)
        {
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            errorMessage = e.getMessage();
            System.err.println(errorMessage);
        }

        // return result of executeUpdate()
        return result;
    }



    ///////////////////////////////////////////////////////////////////////////
    // set parameters for prepared statement, setXXX()
    //It will cast each param to the original data type before calling setXXX()
    ///////////////////////////////////////////////////////////////////////////
    private void setParametersForPreparedStatement(String params)
    {
        // reset
        errorMessage = "";
        Object param = null;    // parameter element in the list

        // check params: if null, do nothing 
        if(params == null)
            return;

        try
        {
                    activeStatement.setString(1, params);
        }
        catch(SQLException e)
        {
            errorMessage = e.getSQLState() + ": " + e.getMessage();
            System.err.println(errorMessage);
        }
        catch(Exception e)
        {
            errorMessage = e.getMessage();
            System.err.println(errorMessage);
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    // return the current error message
    ///////////////////////////////////////////////////////////////////////////
    public String getErrorMessage()
    {
        return errorMessage;
    }



    ///////////////////////////////////////////////////////////////////////////
    // register the JDBC driver based on URL
    ///////////////////////////////////////////////////////////////////////////
    private void initJdbcDriver(String url)
    {
        // test various databases
        try
        {
            if(url.contains("jdbc:mysql"))
                Class.forName("com.mysql.jdbc.Driver");

            else if(url.contains("jdbc:oracle"))
                Class.forName("oracle.jdbc.OracleDriver");

            else if(url.contains("jdbc:derby"))
                Class.forName("org.apache.derby.jdbc.ClientDriver");

            else if(url.contains("jdbc:db2"))
                Class.forName("com.ibm.db2.jcc.DB2Driver");

            else if(url.contains("jdbc:postgresql"))
                Class.forName("org.postgresql.Driver");

            else if(url.contains("jdbc:sqlite"))
                Class.forName("org.sqlite.JDBC");

            else if(url.contains("jdbc:sqlserver"))
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            else if(url.contains("jdbc:sybase"))
                Class.forName("sybase.jdbc.sqlanywhere.IDriver");
        }
        catch(ClassNotFoundException e)
        {
            errorMessage = "Failed to initialize JDBC driver class.";
            System.err.println(errorMessage);
        }
    }

}
