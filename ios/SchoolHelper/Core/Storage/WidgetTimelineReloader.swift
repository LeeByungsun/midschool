import Foundation

#if canImport(WidgetKit)
import WidgetKit
#endif

protocol WidgetTimelineReloading {
    func reloadAllTimelines()
}

struct WidgetTimelineReloader: WidgetTimelineReloading {
    func reloadAllTimelines() {
#if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
#endif
    }
}
