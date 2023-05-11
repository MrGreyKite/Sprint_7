package ru.bagmet;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.bagmet.data.OrderData;

import static io.restassured.RestAssured.given;

public class OrderClient extends RestClient {

    private static final String ORDER_PATH_BY_TRACK = "/orders/track";
    private static final String ORDERS_PATH = "/orders";
    private static final String ORDER_ACCEPT = "/orders/accept";
    private static final String ORDER_FINISH = "/orders/finish";
    private static final String ORDER_CANCEL = "/orders/cancel";

    @Step("Создание заказа")
    public ValidatableResponse createOrder(OrderData order) {
        return given().
                spec(getBaseSpec()).
                body(order).
                when().
                post(ORDERS_PATH).
                then();
    }

    @Step("Получение заказа по трек-номеру '{trackNumber}'")
    public ValidatableResponse getOrderByTrack(int trackNumber) {
        //если передается 0, то преобразовывается в пустую строку
        String track = trackNumber > 0 ? String.valueOf(trackNumber) : "";
        return given().
                spec(getBaseSpec()).
                queryParam("t", track).
                when().
                get(ORDER_PATH_BY_TRACK).
                then();
    }

    @Step("Получение списка всех заказов")
    public ValidatableResponse getOrders() {
        return given().
                spec(getBaseSpec()).
                when().
                get(ORDERS_PATH).
                then();
    }

    @Step("Получение списка заказов для курьера с ID '{courierID}'")
    public ValidatableResponse getOrders(int courierID) {
        return given().
                spec(getBaseSpec()).
                queryParam("courierId", courierID).
                when().
                get(ORDERS_PATH).
                then();
    }

    @Step("Принятие заказа '{orderID}' курьером с ID '{courierID}'")
    public ValidatableResponse acceptOrder(int orderID, int courierID) {
        //если передается 0, то преобразовывается в пустую строку
        String orderId = orderID > 0 ? String.valueOf(orderID) : "";
        String courierId = courierID > 0 ? String.valueOf(courierID) : "";
        return given().
                spec(getBaseSpec()).
                pathParam("id", orderId).
                queryParam("courierId", courierId).
                when().
                put(ORDER_ACCEPT + "/{id}").
                then();
    }

    @Step("Завершение заказа '{orderID}'")
    public ValidatableResponse finishOrder(int orderID) {
        return given().
                spec(getBaseSpec()).
                pathParam("id", orderID).
                when().
                put(ORDER_FINISH + "/{id}").
                then();
    }

    @Step("Отмена заказа по трек-номеру '{trackNumber}'")
    public ValidatableResponse cancelOrder(int trackNumber) {
        return given().
                spec(getBaseSpec()).
                queryParam("track", trackNumber).
                when().
                put(ORDER_CANCEL).
                then().log().all();
    }

}
