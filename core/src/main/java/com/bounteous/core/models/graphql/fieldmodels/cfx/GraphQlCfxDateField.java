package com.bounteous.core.models.graphql.fieldmodels.cfx;

import com.bounteous.core.common.GraphQLServiceException;
import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;
import com.bounteous.core.models.graphql.fieldmodels.adobe.GraphQlDateField;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.FragmentData;
import graphql.execution.DataFetcherResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlCfxDateField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlCfxDateField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/cfx-datetime";
    public static final String TIME_GRAPHQL_TYPE = "Time";
    public static final String DATE_GRAPHQL_TYPE = "Date";

    @Override
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        FragmentData fieldData = contentElement.getValue();
        if (fieldData != null) {
            String value = (String) fieldData.getValue();
            if (value != null) {
                if (value.length() <= 6) {
                    return DataFetcherResult.newResult()
                        .data(valueToTimeStructure(value))
                        .build();
                }
                if (value.length() <= 10) {
                    return DataFetcherResult.newResult()
                        .data(valueToDateStructure(value))
                        .build();
                }
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mmXXX");
                    Date date = sdf.parse(value);
                    GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime(date);
                    return DataFetcherResult.newResult()
                        .data(GraphQlDateField.dateToDateTimeStructure(calendar)).build();
                } catch (ParseException e) {
                    throw new GraphQLServiceException("Unable to parse date", e);
                }
            }
        }

        return DataFetcherResult.newResult()
            .data(null)
            .build();
    }

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private boolean multiple;

    @Inject
    private String type;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return multiple;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getGraphQLType() {
        if ("time".equals(type)) {
            return GraphQlCfxDateField.TIME_GRAPHQL_TYPE;
        } else if ("date".equals(type)) {
            return GraphQlCfxDateField.DATE_GRAPHQL_TYPE;
        } else {
            return GraphQlDateField.DATETIME_GRAPHQL_TYPE;
        }
    }

    public Map<String,Object> valueToDateStructure(String value) {
        if (value == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(value);
            calendar.setTime(date);
        } catch (ParseException pe) {
            return null;
        }
        HashMap<String,Object> dateMap = new HashMap<>();
        dateMap.put(GraphQlDateField.YEAR, calendar.get(Calendar.YEAR));
        dateMap.put(GraphQlDateField.MONTH, calendar.get(Calendar.MONTH) + 1);
        dateMap.put(GraphQlDateField.DAY, calendar.get(Calendar.DAY_OF_MONTH));
        dateMap.put(GraphQlDateField.TIMESTAMP, calendar.getTime().getTime());
        dateMap.put(GraphQlDateField.FORMATTED, value);
        return dateMap;
    }

    public Map<String,Object> valueToTimeStructure(String value) {
        if (value == null) {
            return null;
        }
        String[] values = value.split(":");
        HashMap<String,Object> timeMap = new HashMap<>();
        timeMap.put(GraphQlDateField.HOUR, Integer.parseInt(values[0]));
        timeMap.put(GraphQlDateField.MINUTE, Integer.parseInt(values[1]));
        timeMap.put(GraphQlDateField.SECOND, 0);
        timeMap.put(GraphQlDateField.FORMATTED, value);
        return timeMap;
    }
}
