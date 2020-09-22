package com.bounteous.core.servlets.graphql;

import com.bounteous.core.services.GraphQLService;

import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.sling.api.servlets.HttpConstants.METHOD_GET;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;

@Component(service = Servlet.class,
    property = {
            SERVICE_DESCRIPTION             + "=" + "AEM CFX GraphQL API Servlet",
            SLING_SERVLET_METHODS           + "=" + "[" + METHOD_GET + "," + METHOD_POST + "]",
            SLING_SERVLET_PATHS             + "=" + "/bin/cfx/graphql"
    })
public class GraphQLServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLServlet.class);

    @Reference
    public GraphQLService graphQLService;

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException {
        final String query = request.getParameter("query");
        List<String> headerNames = Collections.list(request.getHeaderNames());
        LOG.debug(headerNames.toString());
        doQuery(query, response);
    }

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException {
        final String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        try {
            final JSONObject jsonObject =  new JSONObject(body);
            if (jsonObject.has("query")) {
                doQuery(jsonObject.getString("query"), response);
            } else {
                doQuery(StringUtils.EMPTY, response);
            }
        } catch (JSONException e) {
            LOG.error("Error paring JSON.", e);
        }
    }

    @Override
    protected void doOptions(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws IOException {
        LOG.debug("GraphQLServlet.doOptions()");
    }

    protected void doQuery(@NotNull final String query, @NotNull final SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(graphQLService.executeQueryAsJson(query));
    }
}