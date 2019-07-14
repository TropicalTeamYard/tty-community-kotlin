package util

internal enum class UserGroup {
    SUPER_ADMIN,
    NORMAL_ADMIN,

    NORMAL_USER,

    GUEST
}

internal enum class UserCertification {
    NORMAL,
    PERSONAL,
    ENTERPRISE
}

internal enum class LoginType {
    PASSWORD,
    TOKEN,
    THIRD_PARTY
}

internal enum class RegisterType {
    THIRD_PARTY,
    NORMAL
}