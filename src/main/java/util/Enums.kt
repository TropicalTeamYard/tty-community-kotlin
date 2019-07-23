package util

enum class UserGroup {
    SUPER_ADMIN,
    NORMAL_ADMIN,

    NORMAL_USER,

    GUEST
}

enum class UserCertification {
    NORMAL,
    PERSONAL,
    ENTERPRISE
}
enum class LoginPlatform{
    WEB, PC, MOBILE, PAD
}

enum class LoginType {
    ID, NICKNAME,
    THIRD_PARTY
}

enum class RegisterType {
    THIRD_PARTY,
    NORMAL
}