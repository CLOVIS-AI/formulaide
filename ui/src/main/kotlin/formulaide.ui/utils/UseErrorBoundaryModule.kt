/*
 * Bindings for https://www.npmjs.com/package/use-error-boundary
 * Generated by Dukat, then edited by hand.
 */

@file:JsModule("use-error-boundary")
@file:JsNonModule
@file:Suppress("PropertyName", "unused")

package formulaide.ui.utils

import react.Props

external interface ErrorState {
	var didCatch: Boolean
	var error: Throwable?
}

external interface UseErrorBoundaryState : ErrorState {
	var ErrorBoundary: UseErrorBoundaryWrapper
	var reset: () -> Unit
}

external interface UseErrorBoundaryOptions {
	var onDidCatch: ((error: Any, errorInfo: Any) -> Unit)?
		get() = definedExternally
		set(value) = definedExternally
}

@JsName("default")
external fun useErrorBoundary(options: UseErrorBoundaryOptions = definedExternally): UseErrorBoundaryState

external interface ErrorObject {
	var error: Any
}

external interface ErrorBoundaryProps : Props {
	var onDidCatch: OnDidCatchCallback
	var children: dynamic /* React.ReactNode? | JSX.Element? */
		get() = definedExternally
		set(value) = definedExternally
	var render: (() -> dynamic)?
		get() = definedExternally
		set(value) = definedExternally
	var renderError: ((error: ErrorObject) -> dynamic)?
		get() = definedExternally
		set(value) = definedExternally
}
