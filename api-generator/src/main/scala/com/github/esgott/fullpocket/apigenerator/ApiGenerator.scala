package com.github.esgott.fullpocket.apigenerator

/**
* Unfortunately, neither Google Cloud supports OpenAPI 3.0, nor Tapir supports
 * OpenAPI 2.0. However, this utility can still be a help to generate the
 * OpenAPI yaml.
 */
object ApiGenerator extends App {

  println(Api.generateOpenapi)

}
