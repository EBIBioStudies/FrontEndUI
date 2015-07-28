package uk.ac.ebi.fg.saxon.functions.search;

public interface IQueryInfoAccessor {
    String[] getQueryInfoParameter(Integer queryId, String key);
}
