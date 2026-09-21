package com.ecommerce.api_gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * Wraps the incoming HttpServletRequest so we can add headers
 * (X-User-Id, X-User-Roles, X-Correlation-Id) that downstream services read.
 *
 * HttpServletRequest headers are immutable — we can't call request.addHeader().
 * The wrapper pattern lets us override getHeader/getHeaders to include our additions.
 */
public class HeaderMutatingRequest extends HttpServletRequestWrapper {

    private final Map<String, String> extraHeaders = new HashMap<>();

    public HeaderMutatingRequest(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String name, String value) {
        extraHeaders.put(name.toLowerCase(), value);
    }

    @Override
    public String getHeader(String name) {
        String extra = extraHeaders.get(name.toLowerCase());
        return extra != null ? extra : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String extra = extraHeaders.get(name.toLowerCase());
        if (extra != null) return Collections.enumeration(List.of(extra));
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(extraHeaders.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) names.add(original.nextElement());
        return Collections.enumeration(names);
    }
}
