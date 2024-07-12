package com.test.poc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiExecutor {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Gson gson;


    public JsonObject processApiRequest(JsonObject request){

        List<JsonObject> errorList = getFileAndValidate(request);

        JsonObject response = new JsonObject();

        if(errorList.size() < 1){
            response.addProperty("Response", "Success");
        } else {
            response.addProperty("Response", "Failure");
            response.add("errorList", gson.toJsonTree(errorList));
        }

        return response;
    }

    public JsonObject processApiRequestForClearH2Db(JsonObject request){
        JdbcTemplate jdbcTemplate = getJdbcTemplateBasedOnDB(request.get("dbType").getAsString());

        jdbcTemplate.execute("DROP ALL OBJECTS");

        JsonObject response = new JsonObject();

        response.addProperty("Response", "Success");

        return response;
    }

    private List<JsonObject> getFileAndValidate(JsonObject request){
        JdbcTemplate jdbcTemplate = getJdbcTemplateBasedOnDB(request.get("dbType").getAsString());

        String queryFilePath = request.get("queryFilePath").getAsString();
        boolean skipOnError = request.has("skipOnError") ?
                request.get("skipOnError").getAsBoolean() : true;

        List<JsonObject> errorList = new ArrayList<>();

        try {
            if (Paths.get(queryFilePath).toFile().exists()) {
                String content = new String(Files.readAllBytes(Paths.get(queryFilePath)));

                List<String> queryListFirstLevel = Arrays.asList(content.split("/"));

                validateQueryFirstLevel(jdbcTemplate, queryListFirstLevel, skipOnError, errorList);
            } else {
                throw new RuntimeException("Given file path does not exist");
            }
        } catch(Exception ex){
            ex.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("reason", ex.getMessage());
            errorList.add(error);
        }

        return errorList;
    }

    private void validateQueryFirstLevel(JdbcTemplate jdbcTemplate, List<String> queryListFirstLevel,
                                         boolean skipOnError, List<JsonObject> errorList){
        for (int i=0;i<queryListFirstLevel.size();i++) {
            if(!skipOnError && errorList.size() > 0)
                break;

            if(queryListFirstLevel.get(i).toLowerCase().contains("begin") ||
                    queryListFirstLevel.get(i).toLowerCase().contains("declare")) {
                executeQuery(jdbcTemplate, queryListFirstLevel.get(i), errorList, i+1);
            } else {
                validateQuerySecondLevel(jdbcTemplate,
                        Arrays.asList(queryListFirstLevel.get(i).split(";")), i,
                        skipOnError, errorList);
            }
        }
    }

    private void validateQuerySecondLevel(JdbcTemplate jdbcTemplate, List<String> queryListSecondLevel,
                                          int level, boolean skipOnError, List<JsonObject> errorList){
        for (int i=0;i<queryListSecondLevel.size();i++) {
            if(!skipOnError && errorList.size() > 0)
                break;
            executeQuery(jdbcTemplate, queryListSecondLevel.get(i), errorList, level + i + 1);
        }
    }

    private void executeQuery(JdbcTemplate jdbcTemplate, String query, List<JsonObject> errorList, int queryId){
        try {
            jdbcTemplate.execute(query);
        } catch (Exception ex){
            JsonObject queryErrorDet = new JsonObject();
            queryErrorDet.addProperty("queryId", queryId);
            queryErrorDet.addProperty("reason", ex.getCause().getMessage().replaceAll("[\\t\\n\\r]+",""));
            queryErrorDet.addProperty("queryStatement", query.replaceAll("[\\t\\n\\r]+",""));
            errorList.add(queryErrorDet);
        }
    }

    private JdbcTemplate getJdbcTemplateBasedOnDB(String dbType){
        switch (dbType.toLowerCase()){
            case "oracle":
                return applicationContext.getBean("jdbcForOracle", JdbcTemplate.class);
            case "postgres":
                return applicationContext.getBean("jdbcForPostgres", JdbcTemplate.class);
            case "mariadb":
                return applicationContext.getBean("jdbcForMariadb", JdbcTemplate.class);
            case "mssql":
                return applicationContext.getBean("jdbcForMssql", JdbcTemplate.class);
            default:
                throw new RuntimeException("Invalid DB Type");
        }
    }
}
