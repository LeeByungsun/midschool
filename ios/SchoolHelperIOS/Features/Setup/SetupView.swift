import SwiftUI

struct SetupView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("학교 설정") {
                    Text("학교 검색/선택")
                    Text("학년/반 입력")
                }
            }
            .navigationTitle("초기 설정")
        }
    }
}
