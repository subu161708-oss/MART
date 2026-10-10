
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
import java.sql.SQLException;

@WebServlet("/add-review")
public class ReviewServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }

        String productIdText = request.getParameter("productId");
        String ratingText = request.getParameter("rating");
        String comment = request.getParameter("comment");

        try {
            int userId = (Integer) session.getAttribute("userId");
            int productId = Integer.parseInt(productIdText);
            int rating = Integer.parseInt(ratingText);

            if (productId <= 0 || rating < 1 || rating > 5) {
                response.sendError(400, "Invalid product ID or rating.");
                return;
            }

            if (comment != null && comment.length() > 2000) {
                response.sendError(400, "Review is too long.");
                return;
            }

            String sql = """
                    INSERT INTO reviews
                    (product_id, user_id, rating, comment)
                    SELECT p.id, ?, ?, ?
                    FROM products p
                    WHERE p.id = ?
                    """;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, userId);
                ps.setInt(2, rating);
                ps.setString(3, comment == null ? "" : comment.trim());
                ps.setInt(4, productId);

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    response.sendError(404, "Product not found.");
                    return;
                }

                response.sendRedirect("products.html?review=success");
            }

        } catch (NumberFormatException e) {
            response.sendError(400, "Please enter valid product and rating values.");
        } catch (SQLException e) {
            log("Unable to save review", e);
            response.sendError(500, "Unable to save review. Check database setup.");
        }
    }
}

