
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (action == null || email == null || password == null
                || email.isBlank() || password.isBlank()) {
            response.sendError(400, "Please enter email and password.");
            return;
        }

        if ("register".equals(action)) {
            registerUser(request, response, email, password);
        } else {
            loginUser(request, response, email, password);
        }
    }

    private void registerUser(HttpServletRequest request,
                              HttpServletResponse response,
                              String email, String password)
            throws IOException {

        String name = request.getParameter("name");

        if (name == null || name.isBlank()) {
            response.sendError(400, "Please enter your name.");
            return;
        }

        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, password);

            ps.executeUpdate();
            response.sendRedirect("login.html?registered=true");

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            response.sendError(409, "This email is already registered.");
        } catch (Exception e) {
            log("Registration failed", e);
            response.sendError(500, "Registration failed. Check database setup.");
        }
    }

    private void loginUser(HttpServletRequest request,
                           HttpServletResponse response,
                           String email, String password)
            throws IOException {

        String sql = "SELECT id, name, password, role FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && password.equals(rs.getString("password"))) {

                    HttpSession session = request.getSession();
                    session.setAttribute("userId", rs.getInt("id"));
                    session.setAttribute("userName", rs.getString("name"));
                    session.setAttribute("role", rs.getString("role"));

                    if ("ADMIN".equalsIgnoreCase(rs.getString("role"))) {
                        response.sendRedirect("admin.html");
                    } else if ("SELLER".equalsIgnoreCase(rs.getString("role"))) {
                        response.sendRedirect("seller.html");
                    } else {
                        response.sendRedirect("home.html");
                    }

                } else {
                    response.sendRedirect("login.html?error=invalid");
                }
            }

        } catch (Exception e) {
            log("Login failed", e);
            response.sendError(500, "Login failed. Check database connection.");
        }
    }
}

