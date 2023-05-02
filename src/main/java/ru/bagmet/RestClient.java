package ru.bagmet;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class RestClient {
    private static final String BASE_URI = "https://qa-scooter.praktikum-services.ru";

    protected RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder().
                setBaseUri(BASE_URI).
                setBasePath("/api/v1").
                setContentType(ContentType.JSON).
                log(LogDetail.ALL).
                build();
    }

    protected ResponseSpecification getResponseSpecForOK() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .log(LogDetail.BODY)
                .build();
    }

    protected ResponseSpecification getResponseSpecForCreated() {
        return new ResponseSpecBuilder()
                .expectStatusCode(201)
                .expectContentType(ContentType.JSON)
                .log(LogDetail.BODY)
                .build();
    }

    protected ResponseSpecification getResponseSpecForGenericError() {
        return new ResponseSpecBuilder()
                .expectStatusCode(400)
                .expectContentType(ContentType.JSON)
                .log(LogDetail.BODY)
                .build();
    }

}
