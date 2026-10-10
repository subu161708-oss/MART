
package com.subamart.controller;

import com.subamart.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/my-orders")
public class OrderListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, total, status, created_at " +
                     "FROM orders WHERE user_id = ? ORDER BY id DESC")) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery();
                 var out = response.getWriter()) {

                out.println("<!DOCTYPE html>");
                out.println("<html lang='en'><head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
                out.println("<title>My Orders - Suba Mart</title>");

                out.println("<style>");
                out.println("body{font-family:Arial,sans-serif;background:#f5f3ff;margin:0;padding:25px}");
                out.println(".container{max-width:900px;margin:auto;background:white;padding:25px;border-radius:14px}");
                out.println("h1{color:#6d28d9}");
                out.println("table{width:100%;border-collapse:collapse;margin-top:20px}");
                out.println("th,td{padding:12px;text-align:left;border-bottom:1px solid #ddd}");
                out.println("th{background:#6d28d9;color:white}");
                out.println("a{color:#6d28d9;text-decoration:none}");
                out.println("</style></head><body>");

                out.println("<div class='container'>");
                out.println("<h1>Suba Mart - My Orders</h1>");
                out.println("<p><a href='home.html'>&#8592; Back to Home</a></p>");

                out.println("<table>");
                out.println("<tr><th>Order ID</th><th>Total</th><th>Status</th><th>Date</th></tr>");

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("id") + "</td>");
                    out.println("<td>Rs. " +
                            rs.getBigDecimal("total").toPlainString() + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("status")) + "</td>");
                    out.println("<td>" + rs.getTimestamp("created_at") + "</td>");
                    out.println("</tr>");
                }

                if (!found) {
                    out.println("<tr><td colspan='4'>No orders found.</td></tr>");
                }

                out.println("</table></div></body></html>");
            }

        } catch (Exception e) {
            log("Unable to retrieve orders", e);
            response.sendError(500, "Unable to load your orders.");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}

