
package com.subamart.controller;

import com.subamart.util.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/place-order")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String productIdValue = request.getParameter("productId");
        String quantityValue = request.getParameter("quantity");

        try {
            int productId = Integer.parseInt(productIdValue);
            int quantity = Integer.parseInt(quantityValue);

            if (productId <= 0 || quantity <= 0 || quantity > 100) {
                response.sendError(400, "Invalid product or quantity.");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);

                try {
                    String productSql =
                            "SELECT price, stock FROM products WHERE id = ? FOR UPDATE";

                    BigDecimal price;
                    int stock;

                    try (PreparedStatement ps =
                                 con.prepareStatement(productSql)) {
                        ps.setInt(1, productId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                response.sendError(404, "Product not found.");
                                con.rollback();
                                return;
                            }

                            price = rs.getBigDecimal("price");
                            stock = rs.getInt("stock");
                        }
                    }

                    if (stock < quantity) {
                        con.rollback();
                        response.sendError(409, "Insufficient stock.");
                        return;
                    }

                    BigDecimal total =
                            price.multiply(BigDecimal.valueOf(quantity));

                    String stockSql =
                            "UPDATE products SET stock = stock - ? " +
                            "WHERE id = ? AND stock >= ?";

                    try (PreparedStatement ps =
                                 con.prepareStatement(stockSql)) {
                        ps.setInt(1, quantity);
                        ps.setInt(2, productId);
                        ps.setInt(3, quantity);

                        if (ps.executeUpdate() != 1) {
                            con.rollback();
                            response.sendError(409, "Stock unavailable.");
                            return;
                        }
                    }

                    String orderSql =
                            "INSERT INTO orders (user_id, total, status) " +
                            "VALUES (?, ?, 'PLACED')";

                    try (PreparedStatement ps =
                                 con.prepareStatement(orderSql)) {
                        ps.setInt(1, userId);
                        ps.setBigDecimal(2, total);
                        ps.executeUpdate();
                    }

                    con.commit();
                    response.sendRedirect("home.html?order=success");

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid product ID or quantity.");
        } catch (Exception e) {
            log("Order placement failed", e);
            response.sendError(500, "Unable to place order.");
        }
    }
}

