package formulaide.ui.utils

import formulaide.ui.utils.DelegatedProperty.Companion.DelegatedProperty
import react.ChildrenBuilder
import react.useEffectOnce
import react.useState

/**
 * Implementation of the Observer pattern.
 *
 * See [useGlobalState].
 */
class GlobalState<T>(initialValue: T) {
	var value = initialValue
		set(new) {
			field = new
			subscribers.forEach { (_, it) -> it(new) }
		}

	val subscribers = mutableListOf<Pair<String?, (T) -> Unit>>()

	fun asDelegated() = DelegatedProperty(
		get = { value },
		onUpdate = { valueGenerator -> value = valueGenerator(value) },
	)
}

/**
 * A React hack that allows to use global state in components.
 *
 * This hook can be used with the same syntax as [useState], and will handle all the machinery for the component to be updated at the right time.
 */
@Suppress("UnusedReceiverParameter") // ChildrenBuilder for type safety
fun <T> ChildrenBuilder.useGlobalState(
	globalState: GlobalState<T>,
	interceptor: WriteDelegatedProperty<T>? = null,
	name: String? = null,
): WriteDelegatedProperty<T> {
	/*
	 * Implementation details:
	 * - We create a local state with useState
	 * - When the global state is edited, we edit the local state as well (so React re-renders that component)
	 * - Don't forget to unsubscribe if we disappear
	 *
	 * The local state is just used to tell React when to refresh. Its value is never used.
	 *
	 * Inspired by https://dev.to/yezyilomo/global-state-management-in-react-with-global-variables-and-hooks-state-management-doesn-t-have-to-be-so-hard-2n2c
	 */

	val delegated = interceptor ?: globalState.asDelegated()

	val (_, setLocal) = useState(delegated.value)

	val subscriber = name to { new: T -> setLocal(new) }

	useEffectOnce {
		globalState.subscribers.add(subscriber)

		cleanup {
			globalState.subscribers.remove(subscriber)
		}
	}

	return WriteDelegatedProperty(
		reader = delegated.reader,
		onUpdate = { valueGenerator -> delegated.update(valueGenerator) }
	)
}
