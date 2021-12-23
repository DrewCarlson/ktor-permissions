package org.drewcarlson.ktor.permissions

import io.ktor.server.routing.*

class AuthorizedRouteSelector(private val description: String) : RouteSelector() {

    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Constant

    override fun toString(): String = "(authorize ${description})"
}
