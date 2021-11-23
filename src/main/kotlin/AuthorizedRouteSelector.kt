package org.drewcarlson.ktor.permissions

import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {

    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Constant

    override fun toString(): String = "(authorize ${description})"
}
