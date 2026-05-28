import Foundation

struct NEISClient {
    struct Config {
        let baseURL: URL
        let apiKey: String
        let noticesBaseURL: URL

        static func fromEnvironment(_ environment: [String: String] = ProcessInfo.processInfo.environment) -> Config? {
            let baseURLString = environment["NEIS_BASE_URL"]?.trimmingCharacters(in: .whitespacesAndNewlines)
            let noticesBaseURLString = environment["WEB_BASE_URL"]?.trimmingCharacters(in: .whitespacesAndNewlines)

            guard
                let baseURL = URL(string: baseURLString?.isEmpty == false ? baseURLString! : "https://open.neis.go.kr/"),
                let noticesBaseURL = URL(string: noticesBaseURLString?.isEmpty == false ? noticesBaseURLString! : "https://midschool.vercel.app/")
            else {
                return nil
            }

            return Config(
                baseURL: baseURL,
                apiKey: environment["NEIS_API_KEY"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
                noticesBaseURL: noticesBaseURL
            )
        }
    }

    enum ClientError: LocalizedError {
        case invalidConfiguration
        case invalidRequest(String)
        case invalidResponse
        case network(Int)
        case neis(String)

        var errorDescription: String? {
            switch self {
            case .invalidConfiguration:
                return "iOS NEIS 설정이 잘못되었어요."
            case .invalidRequest(let endpoint):
                return "\(endpoint) 요청을 만들지 못했어요."
            case .invalidResponse:
                return "서버 응답을 읽지 못했어요."
            case .network(let status):
                return "NEIS 응답이 실패했어요. (\(status))"
            case .neis(let message):
                return message
            }
        }
    }

    private let session: URLSession
    private let config: Config?

    init(
        session: URLSession = .shared,
        config: Config? = Config.fromEnvironment()
    ) {
        self.session = session
        self.config = config
    }

    func fetchMeals(
        officeCode: String,
        schoolCode: String,
        date: String
    ) async throws -> [MealInfo] {
        let response: NeisResponse<MealRowDto> = try await fetchNeisJSON(
            endpoint: "hub/mealServiceDietInfo",
            params: ["MLSV_YMD": date],
            officeCode: officeCode,
            schoolCode: schoolCode
        )

        return try extractRows(
            rootResult: response.result,
            sections: response.mealServiceDietInfo,
            label: "급식"
        ).map {
            MealInfo(
                date: $0.date,
                mealType: $0.mealType ?? "",
                menu: $0.menu ?? "",
                calorieInfo: $0.calorieInfo ?? ""
            )
        }
    }

    func fetchSchedule(
        officeCode: String,
        schoolCode: String,
        month: String
    ) async throws -> [SchoolEvent] {
        let response: NeisResponse<ScheduleRowDto> = try await fetchNeisJSON(
            endpoint: "hub/SchoolSchedule",
            params: ["AA_YMD": month],
            officeCode: officeCode,
            schoolCode: schoolCode
        )

        return try extractRows(
            rootResult: response.result,
            sections: response.schoolSchedule,
            label: "학사 일정"
        ).map {
            SchoolEvent(
                date: $0.date,
                title: $0.title ?? "",
                description: $0.description ?? ""
            )
        }
    }

    func fetchTimetable(
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classroom: String,
        date: String
    ) async throws -> [TimetableItem] {
        let endpoint = timetableEndpoint(for: schoolKind)
        let response: NeisResponse<TimetableRowDto> = try await fetchNeisJSON(
            endpoint: endpoint,
            params: [
                "GRADE": grade,
                "CLASS_NM": classroom,
                "ALL_TI_YMD": date,
            ],
            officeCode: officeCode,
            schoolCode: schoolCode
        )

        let sections: [NeisSection<TimetableRowDto>]?
        switch endpoint {
        case "hub/elsTimetable":
            sections = response.elsTimetable
        case "hub/hisTimetable":
            sections = response.hisTimetable
        default:
            sections = response.misTimetable
        }

        return try extractRows(
            rootResult: response.result,
            sections: sections,
            label: "시간표"
        ).map {
            TimetableItem(
                date: $0.date,
                period: $0.period ?? "",
                subject: $0.subject ?? "",
                grade: $0.grade ?? "",
                classroom: $0.classroom ?? ""
            )
        }
    }

    func searchSchools(query: String) async throws -> [SchoolInfo] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return [] }

        let response: NeisResponse<SchoolInfoRowDto> = try await fetchNeisJSON(
            endpoint: "hub/schoolInfo",
            params: ["SCHUL_NM": trimmed],
            includeSchoolContext: false
        )

        return try extractRows(
            rootResult: response.result,
            sections: response.schoolInfo,
            label: "학교 검색"
        )
        .map {
            SchoolInfo(
                officeCode: $0.officeCode,
                officeName: $0.officeName ?? "",
                schoolCode: $0.schoolCode,
                schoolName: $0.schoolName ?? "",
                schoolKind: $0.schoolKind ?? "",
                roadAddress: $0.roadAddress ?? ""
            )
        }
        .filter {
            ["초등학교", "중학교", "고등학교"].contains($0.schoolKind)
        }
    }

    private func fetchNeisJSON<Response: Decodable>(
        endpoint: String,
        params: [String: String],
        includeSchoolContext: Bool = true,
        officeCode: String? = nil,
        schoolCode: String? = nil
    ) async throws -> Response {
        guard let config else { throw ClientError.invalidConfiguration }
        guard let url = URL(string: endpoint, relativeTo: config.baseURL) else {
            throw ClientError.invalidRequest(endpoint)
        }
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
            throw ClientError.invalidRequest(endpoint)
        }

        var queryItems = [
            URLQueryItem(name: "Type", value: "json"),
            URLQueryItem(name: "pIndex", value: "1"),
            URLQueryItem(name: "pSize", value: "100"),
        ]

        if !config.apiKey.isEmpty {
            queryItems.insert(URLQueryItem(name: "KEY", value: config.apiKey), at: 0)
        }

        if includeSchoolContext {
            if let officeCode, !officeCode.isEmpty {
                queryItems.append(URLQueryItem(name: "ATPT_OFCDC_SC_CODE", value: officeCode))
            }
            if let schoolCode, !schoolCode.isEmpty {
                queryItems.append(URLQueryItem(name: "SD_SCHUL_CODE", value: schoolCode))
            }
        }

        queryItems.append(contentsOf: params.compactMap { key, value in
            value.isEmpty ? nil : URLQueryItem(name: key, value: value)
        })
        components.queryItems = queryItems

        guard let requestURL = components.url else {
            throw ClientError.invalidRequest(endpoint)
        }

        let (data, response) = try await session.data(from: requestURL)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ClientError.invalidResponse
        }
        guard 200..<300 ~= httpResponse.statusCode else {
            throw ClientError.network(httpResponse.statusCode)
        }

        do {
            return try JSONDecoder().decode(Response.self, from: data)
        } catch {
            throw ClientError.invalidResponse
        }
    }

    private func extractRows<Row>(
        rootResult: NeisResultDto?,
        sections: [NeisSection<Row>]?,
        label: String
    ) throws -> [Row] {
        try validateNeisResult(rootResult, label: label)
        let sectionResult = sections?
            .flatMap { $0.head ?? [] }
            .compactMap { $0.result }
            .first
        try validateNeisResult(sectionResult, label: label)
        return sections?.dropFirst().first?.row ?? []
    }

    private func validateNeisResult(_ result: NeisResultDto?, label: String) throws {
        let code = result?.code?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !code.isEmpty else { return }
        guard code == "INFO-000" || code == "INFO-200" else {
            let message: String
            switch code {
            case "INFO-100":
                message = "\(label) 조회에 필요한 값이 누락되었어요."
            case "INFO-300":
                message = "\(label) 데이터가 아직 준비되지 않았어요."
            case "ERROR-300":
                message = "나이스 인증키를 다시 확인해 주세요."
            case "ERROR-336":
                message = "요청 횟수가 많아 잠시 후 다시 시도해 주세요."
            default:
                let fallback = result?.message?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                message = fallback.isEmpty ? "\(label) 정보를 불러오지 못했어요." : fallback
            }
            throw ClientError.neis(message)
        }
    }

    private func timetableEndpoint(for schoolKind: String) -> String {
        switch schoolKind.trimmingCharacters(in: .whitespacesAndNewlines) {
        case "초등학교":
            return "hub/elsTimetable"
        case "고등학교":
            return "hub/hisTimetable"
        default:
            return "hub/misTimetable"
        }
    }
}

struct NoticesClient {
    enum ClientError: LocalizedError {
        case invalidConfiguration
        case invalidRequest
        case invalidResponse
        case network(Int)
        case server(String)

        var errorDescription: String? {
            switch self {
            case .invalidConfiguration:
                return "가정통신문 서버 설정이 잘못되었어요."
            case .invalidRequest:
                return "가정통신문 요청을 만들지 못했어요."
            case .invalidResponse:
                return "가정통신문 응답을 읽지 못했어요."
            case .network(let status):
                return "가정통신문 서버 응답이 실패했어요. (\(status))"
            case .server(let message):
                return message
            }
        }
    }

    private let session: URLSession
    private let config: NEISClient.Config?

    init(
        session: URLSession = .shared,
        config: NEISClient.Config? = .fromEnvironment()
    ) {
        self.session = session
        self.config = config
    }

    func fetchNotices(
        officeCode: String,
        schoolCode: String,
        limit: Int
    ) async throws -> [NoticePreview] {
        guard let config else { throw ClientError.invalidConfiguration }
        guard var components = URLComponents(url: config.noticesBaseURL.appendingPathComponent("api/notices"), resolvingAgainstBaseURL: false) else {
            throw ClientError.invalidRequest
        }
        components.queryItems = [
            URLQueryItem(name: "officeCode", value: officeCode),
            URLQueryItem(name: "schoolCode", value: schoolCode),
            URLQueryItem(name: "limit", value: String(max(1, min(limit, 10)))),
        ]
        guard let url = components.url else {
            throw ClientError.invalidRequest
        }

        let (data, response) = try await session.data(from: url)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ClientError.invalidResponse
        }
        guard 200..<300 ~= httpResponse.statusCode else {
            throw ClientError.network(httpResponse.statusCode)
        }

        let payload: NoticeListResponse
        do {
            payload = try JSONDecoder().decode(NoticeListResponse.self, from: data)
        } catch {
            throw ClientError.invalidResponse
        }

        switch payload.status {
        case "success", "empty":
            return payload.items.map {
                NoticePreview(
                    id: $0.id,
                    title: $0.title,
                    date: $0.date,
                    author: $0.author,
                    url: $0.url
                )
            }
        default:
            let message = payload.message?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            throw ClientError.server(message.isEmpty ? "가정통신문을 불러오지 못했어요." : message)
        }
    }
}

private struct NeisResponse<Row: Decodable>: Decodable {
    let result: NeisResultDto?
    let mealServiceDietInfo: [NeisSection<Row>]?
    let schoolSchedule: [NeisSection<Row>]?
    let elsTimetable: [NeisSection<Row>]?
    let misTimetable: [NeisSection<Row>]?
    let hisTimetable: [NeisSection<Row>]?
    let schoolInfo: [NeisSection<Row>]?

    private enum CodingKeys: String, CodingKey {
        case result = "RESULT"
        case mealServiceDietInfo
        case schoolSchedule = "SchoolSchedule"
        case elsTimetable
        case misTimetable
        case hisTimetable
        case schoolInfo
    }
}

private struct NeisSection<Row: Decodable>: Decodable {
    let head: [NeisHeadDto]?
    let row: [Row]?
}

private struct NeisHeadDto: Decodable {
    let result: NeisResultDto?

    private enum CodingKeys: String, CodingKey {
        case result = "RESULT"
    }
}

private struct NeisResultDto: Decodable {
    let code: String?
    let message: String?

    private enum CodingKeys: String, CodingKey {
        case code = "CODE"
        case message = "MESSAGE"
    }
}

private struct MealRowDto: Decodable {
    let date: String
    let mealType: String?
    let menu: String?
    let calorieInfo: String?

    private enum CodingKeys: String, CodingKey {
        case date = "MLSV_YMD"
        case mealType = "MMEAL_SC_NM"
        case menu = "DDISH_NM"
        case calorieInfo = "CAL_INFO"
    }
}

private struct ScheduleRowDto: Decodable {
    let date: String
    let title: String?
    let description: String?

    private enum CodingKeys: String, CodingKey {
        case date = "AA_YMD"
        case title = "EVENT_NM"
        case description = "EVENT_CNTNT"
    }
}

private struct TimetableRowDto: Decodable {
    let date: String
    let period: String?
    let subject: String?
    let grade: String?
    let classroom: String?

    private enum CodingKeys: String, CodingKey {
        case date = "ALL_TI_YMD"
        case period = "PERIO"
        case subject = "ITRT_CNTNT"
        case grade = "GRADE"
        case classroom = "CLASS_NM"
    }
}

private struct SchoolInfoRowDto: Decodable {
    let officeCode: String
    let officeName: String?
    let schoolCode: String
    let schoolName: String?
    let schoolKind: String?
    let roadAddress: String?

    private enum CodingKeys: String, CodingKey {
        case officeCode = "ATPT_OFCDC_SC_CODE"
        case officeName = "ATPT_OFCDC_SC_NM"
        case schoolCode = "SD_SCHUL_CODE"
        case schoolName = "SCHUL_NM"
        case schoolKind = "SCHUL_KND_SC_NM"
        case roadAddress = "ORG_RDNMA"
    }
}

private struct NoticeListResponse: Decodable {
    let status: String
    let items: [NoticeItem]
    let message: String?
}

private struct NoticeItem: Decodable {
    let id: String
    let title: String
    let date: String
    let author: String
    let url: String
}
