package lk.ijse.dep7.pos.pos.service;

import lk.ijse.dep7.pos.pos.dto.ItemDTO;
import lk.ijse.dep7.pos.pos.exception.DuplicateIdentifierException;
import lk.ijse.dep7.pos.pos.exception.FailedOperationException;
import lk.ijse.dep7.pos.pos.exception.NotFoundException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemService {

    private Connection connection;

    public ItemService() {
    }

    public ItemService(Connection connection) {
        this.connection = connection;
    }

    public void saveItem(ItemDTO item) throws DuplicateIdentifierException, FailedOperationException {
        try {

            if (existItem(item.getCode())) {
                throw new DuplicateIdentifierException(item.getCode() + " already exists");
            }

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO item (code, description, unit_price, qty_on_hand) VALUES (?,?,?,?)");
            pstm.setString(1, item.getCode());
            pstm.setString(2, item.getDescription());
            pstm.setBigDecimal(3, item.getUnitPrice());
            pstm.setInt(4, item.getQtyOnHand());
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to save the item", e);
        }
    }

    private boolean existItem(String code) throws SQLException {
        PreparedStatement pstm = connection.prepareStatement("SELECT code FROM item WHERE code=?");
        pstm.setString(1, code);
        return pstm.executeQuery().next();
    }

    public void updateItem(ItemDTO item) throws FailedOperationException, NotFoundException {
        try {

            if (!existItem(item.getCode())) {
                throw new NotFoundException("There is no such item associated with the id " + item.getCode());
            }
            PreparedStatement pstm = connection.prepareStatement("UPDATE item SET description=?, unit_price=?, qty_on_hand=? WHERE code=?");
            pstm.setString(1, item.getDescription());
            pstm.setBigDecimal(2, item.getUnitPrice());
            pstm.setInt(3, item.getQtyOnHand());
            pstm.setString(4, item.getCode());
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to update the item " + item.getCode(), e);
        }
    }

    public void deleteItem(String code) throws NotFoundException, FailedOperationException {
        try {

            if (!existItem(code)) {
                throw new NotFoundException("There is no such item associated with the id " + code);
            }

            PreparedStatement pstm = connection.prepareStatement("DELETE FROM item WHERE code=?");
            pstm.setString(1, code);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to delete the item " + code, e);
        }
    }

    public ItemDTO findItem(String code) throws NotFoundException, FailedOperationException {
        try {

            if (!existItem(code)) {
                throw new NotFoundException("There is no such item associated with the id " + code);
            }

            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM item WHERE code=?");
            pstm.setString(1, code);
            ResultSet rst = pstm.executeQuery();
            rst.next();
            return new ItemDTO(code, rst.getString("description"), rst.getBigDecimal("unit_price"), rst.getInt("qty_on_hand"));
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to find the Item " + code, e);
        }
    }

    public List<ItemDTO> findAllItems() throws FailedOperationException {
        try {
            List<ItemDTO> itemList = new ArrayList<>();
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SELECT * FROM item");

            while (rst.next()) {
                itemList.add(new ItemDTO(rst.getString("code"), rst.getString("description"), rst.getBigDecimal("unit_price"), rst.getInt("qty_on_hand")));
            }

            return itemList;
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to find items", e);
        }
    }

    public List<ItemDTO> findAllItems(int page, int size) throws FailedOperationException {
        try {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM item LIMIT ? OFFSET ?;");
            stm.setObject(1, size);
            stm.setObject(2, size * (page - 1));

            ResultSet rst = stm.executeQuery();
            List<ItemDTO> itemList = new ArrayList<>();

            while (rst.next()) {
                itemList.add(new ItemDTO(rst.getString("code"),
                        rst.getString("description"),
                        rst.getBigDecimal("unit_price"),
                        rst.getInt("qty_on_hand")));
            }
            return itemList;
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to fetch items", e);
        }
    }

    public String generateNewItemCode() throws FailedOperationException {
        try {
            ResultSet rst = connection.createStatement().executeQuery("SELECT code FROM item ORDER BY code DESC LIMIT 1;");

            if (rst.next()) {
                String id = rst.getString("code");
                int newItemId = Integer.parseInt(id.replace("I", "")) + 1;
                return String.format("I%03d", newItemId);
            } else {
                return "I001";
            }
        } catch (SQLException e) {
            throw new FailedOperationException("Failed to generate a new item code", e);
        }
    }

}
