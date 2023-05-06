package ru.bagmet;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.bagmet.data.CourierCredentials;

import static io.restassured.RestAssured.given;

public class CourierClient extends RestClient {

    private static final String COURIER_PATH = "/courier";
    private static final String COURIER_LOGIN_PATH = "/courier/login";
    private static final String COURIER_DATA_PATH = "/courier/{id}/ordersCount";

    @Step("Отправка запроса на создание курьера")
    public ValidatableResponse createCourier(CourierCredentials courier) {
        return given().
                spec(getBaseSpec()).
                body(courier).
                when().
                post(COURIER_PATH).
                then();
    }

    @Step("Отправка запроса на удаление курьера по ID")
    public ValidatableResponse deleteCourierByID(int courierID) {
        return given().
                spec(getBaseSpec()).
                pathParam("id", courierID).
                when().
                delete(COURIER_PATH + "/{id}").
                then();
    }

    @Step("Отправка запроса на удаление курьера по ID")
    public ValidatableResponse deleteCourierByID(String courierID) {
        return given().
                spec(getBaseSpec()).
                pathParam("id", courierID).
                body("{\"id\": " + courierID + "}").
                when().
                delete(COURIER_PATH + "/{id}").
                then();
    }

    @Step("Отправка запроса на авторизацию с данными курьера")
    public ValidatableResponse loginWithCourier(CourierCredentials courier) {
        return given().
                spec(getBaseSpec()).
                body(courier).
                when().
                post(COURIER_LOGIN_PATH).
                then();
    }

}
