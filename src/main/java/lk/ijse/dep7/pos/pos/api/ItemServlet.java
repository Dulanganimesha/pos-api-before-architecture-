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
import lk.ijse.dep7.pos.pos.dto.ItemDTO;
import lk.ijse.dep7.pos.pos.exception.DuplicateIdentifierException;
import lk.ijse.dep7.pos.pos.exception.FailedOperationException;
import lk.ijse.dep7.pos.pos.exception.NotFoundException;
import lk.ijse.dep7.pos.pos.service.ItemService;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ItemServlet", urlPatterns = "/items")
public class ItemServlet extends HttpServlet {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Resource(name = "java:comp/env/jdbc/posCP")
    public DataSource connectionPool;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try (Connection connection = connectionPool.getConnection()) {
            ItemService itemService = new ItemService(connection);
            String code = req.getParameter("code");
            String page = req.getParameter("page");
            String size = req.getParameter("size");
            List<ItemDTO> items = new ArrayList<>();

            if (code == null) {

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
                    items = itemService.findAllItems(p, s);
                } else {
                    items = itemService.findAllItems();
                }
            } else {
                items.add(itemService.findItem(code));
            }

            String json = jsonb.toJson(code == null ? items : items.get(0));
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
            ItemDTO item = jsonb.fromJson(req.getReader(), ItemDTO.class);

            if (item.getCode() == null || !item.getCode().matches("I\\d{3}")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item code can't be empty");
                return;
            } else if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item description can't be empty");
                return;
            } else if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(new BigDecimal(0)) <= 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid unit price");
                return;
            } else if (item.getQtyOnHand() < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid qty on hand");
                return;
            }
            ItemService itemService = new ItemService(connection);

            item.setUnitPrice(item.getUnitPrice().setScale(2));
            itemService.saveItem(item);
            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();
            out.println(jsonb.toJson(item.getCode()));

        } catch (JsonbException exp) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException | RuntimeException | FailedOperationException exp) {
            throw new RuntimeException(exp);
        } catch (DuplicateIdentifierException e) {
            throw new RuntimeException("Item already exits", e);
        }

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (req.getContentType() == null || !req.getContentType().equals("application/json")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            ItemDTO item = jsonb.fromJson(req.getReader(), ItemDTO.class);

            if (item.getCode() == null || !item.getCode().matches("I\\d{3}")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item code can't be empty");
                return;
            } else if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item description can't be empty");
                return;
            } else if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(new BigDecimal(0)) <= 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid unit price");
                return;
            } else if (item.getQtyOnHand() < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid qty on hand");
                return;
            }

            try (Connection connection = connectionPool.getConnection()) {
                ItemService itemService = new ItemService(connection);

                itemService.updateItem(item);
                resp.setContentType("application/json");
                resp.getWriter().println(jsonb.toJson("OK"));

            } catch (SQLException | FailedOperationException ex) {
                throw new RuntimeException(ex);
            } catch (NotFoundException e) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (JsonbException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String code = req.getParameter("code");

        if (code == null || !code.matches("I\\d{3}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection connection = connectionPool.getConnection()) {
            ItemService itemService = new ItemService(connection);
            itemService.deleteItem(code);
            resp.setContentType("application/json");
            resp.getWriter().println(jsonb.toJson("OK"));

        } catch (SQLException | FailedOperationException exp) {
            throw new RuntimeException(exp);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }
}