public class Checklist {

    @TestId(value = "TC-103", order = 3)
    void checkCommentAdded() { }

    @TestId(value = "TC-101", order = 1)
    void checkLogin() { }

    @TestId(value = "TC-104", order = 4)
    void checkLogout() { }

    @TestId(value = "TC-102", order = 2)
    void checkTaskCreated() { }

    // без аннотации
    void helperNotATest() { }
}