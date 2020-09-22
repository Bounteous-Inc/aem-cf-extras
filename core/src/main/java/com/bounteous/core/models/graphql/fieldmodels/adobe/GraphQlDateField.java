package com.bounteous.core.models.graphql.fieldmodels.adobe;

import com.bounteous.core.models.graphql.GraphQLBase;
import com.bounteous.core.models.graphql.fieldmodels.AbstractGraphQlDataTypeField;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.FragmentData;
import graphql.execution.DataFetcherResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

@Model(
        adaptables = { Resource.class, SlingHttpServletRequest.class },
        resourceType = {GraphQlDateField.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class GraphQlDateField extends AbstractGraphQlDataTypeField {
    static final String RESOURCE_TYPE = "settings/dam/cfm/models/formbuilderconfig/datatypes/items/date";
    public static final String DATETIME_GRAPHQL_TYPE = "DateTime";
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
    public static final String TIMESTAMP = "timestamp";
    public static final String FORMATTED = "formatted";

    @Inject
    @Default(values = "")
    private String name;

    @Inject
    private boolean multiple;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMultiValue() {
        return multiple;
    }

    public static Map<String,Object> dateToDateTimeStructure(GregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ");
        HashMap<String,Object> dateTimeMap = new HashMap<>();
        dateTimeMap.put(YEAR, calendar.get(Calendar.YEAR));
        dateTimeMap.put(MONTH, calendar.get(Calendar.MONTH) + 1);
        dateTimeMap.put(DAY, calendar.get(Calendar.DAY_OF_MONTH));
        dateTimeMap.put(HOUR, calendar.get(Calendar.HOUR_OF_DAY));
        dateTimeMap.put(MINUTE, calendar.get(Calendar.MINUTE));
        dateTimeMap.put(SECOND, calendar.get(Calendar.SECOND));
        dateTimeMap.put(TIMESTAMP, calendar.getTime().getTime());
        dateTimeMap.put(FORMATTED, sdf.format(calendar.getTime()));
        return dateTimeMap;
    }

    @Override
    public String getGraphQLType() {
        return DATETIME_GRAPHQL_TYPE;
    }

    @Override
    protected DataFetcherResult getDataFetcherResult(final ContentElement contentElement, GraphQLBase.DataFetcherContext context) {
        FragmentData fieldData = contentElement.getValue();
        if (fieldData != null) {
            GregorianCalendar dateValue = (GregorianCalendar) fieldData.getValue();
            if (dateValue != null) {
                return DataFetcherResult.newResult()
                    .data(dateToDateTimeStructure(dateValue))
                    .build();
            }
        }

        return DataFetcherResult.newResult()
            .data(null)
            .build();
    }
}
