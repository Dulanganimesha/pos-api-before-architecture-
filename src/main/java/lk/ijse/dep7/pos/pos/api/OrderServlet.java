package lk.ijse.dep7.pos.pos.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep7.pos.pos.dto.OrderDTO;
import lk.ijse.dep7.pos.pos.exception.DuplicateIdentifierException;
import lk.ijse.dep7.pos.pos.exception.FailedOperationException;
import lk.ijse.dep7.pos.pos.exception.NotFoundException;
import lk.ijse.dep7.pos.pos.service.OrderService;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "OrderServlet", urlPatterns = "/orders")
public class OrderServlet extends HttpServlet {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Resource(name = "java:comp/env/jdbc/posCP")
    public DataSource connectionPool;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try (Connection connection = connectionPool.getConnection()) {
            OrderService orderService = new OrderService(connection);
            String q = req.getParameter("q");
            String id = req.getParameter("id");
            String page = req.getParameter("page");
            String size = req.getParameter("size");
            List<OrderDTO> orders = new ArrayList<>();

            if (id == null) {

                if (page != null && size != null) {

                    if (!(page.matches("[-]?\\d+") && size.matches("[-]?\\d+"))) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page or size");
                        return;
                    }

                    int p = Integer.parseInt(page);
                    int s = Integer.parseInt(size);

                    if (p <= 0 || s <= 0) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page or size");
                        return;
                    }

                    resp.setHeader("X-Total-Count", orderService.getSearchOrdersCount(q == null ? "" : q) + "");
                    orders = orderService.searchOrders(q == null ? "" : q, p, s);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Page and size should be specified to search");
                    return;
                }
            } else {
                orders.add(orderService.searchOrder(id));
            }

            String json = jsonb.toJson(id == null ? orders : orders.get(0));
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.println(json);

        } catch (SQLException | FailedOperationException ex) {
            throw new RuntimeException(ex);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (req.getContentType() == null || !req.getContentType().equals("application/json")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection connection = connectionPool.getConnection()) {
            OrderDTO order = jsonb.fromJson(req.getReader(), OrderDTO.class);

            if (order.getOrderId() == null || !order.getOrderId().matches("OD\\d{3}")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order id");
                return;
            } else if (order.getOrderDate() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order date");
                return;
            } else if (order.getCustomerId() == null || order.getCustomerId().trim().isEmpty() ||
                    !order.getCustomerId().matches("C\\d{3}")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid customer id");
                return;
            } else if (order.getOrderDetails() == null || order.getOrderDetails().size() < 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order");
                return;
            } else {
                boolean invalid = order.getOrderDetails().stream()
                        .anyMatch(orderDetailDTO ->
                                orderDetailDTO.getItemCode() == null ||
                                        !orderDetailDTO.getItemCode().matches("I\\d{3}") ||
                                        orderDetailDTO.getQty() <= 0 ||
                                        orderDetailDTO.getUnitPrice() == null ||
                                        orderDetailDTO.getUnitPrice().compareTo(new BigDecimal(0)) <= 0

                        );
                if (invalid) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order details");
                    return;
                }
            }

            OrderService orderService = new OrderService(connection);

            orderService.saveOrder(order.getOrderId(), order.getOrderDate(), order.getCustomerId(), order.getOrderDetails());
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.println(jsonb.toJson(order.getOrderId()));

        } catch (JsonbException exp) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException | RuntimeException | FailedOperationException exp) {
            throw new RuntimeException(exp);
        } catch (DuplicateIdentifierException e) {
            throw new RuntimeException("Order already exits", e);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid order details");
        }

    }

}