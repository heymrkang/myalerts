$(function () {
    // 1. 탭 전환 이벤트 (로그인 / 회원가입)
    $('#tab-login').on('click', function () {
        switchAuthTab('login');
    });

    $('#tab-register').on('click', function () {
        switchAuthTab('register');
    });

    // 2. 로그인 비동기 제출 처리
    $('#loginForm').on('submit', function (e) {
        e.preventDefault();

        const userId = $('#loginId').val().trim();
        const userPw = $('#loginPw').val();

        $.ajax({
            url: '/login',
            type: 'POST',
            data: {
                userId: userId,
                userPw: userPw
            },
            success: function (res) {
                if (res.success) {
                    showAlert('로그인 성공. 새로고침 중...', true);
                    setTimeout(function () {
                        location.reload();
                    }, 300);
                } else {
                    showAlert(res.message || '로그인에 실패했습니다.', false);
                }
            },
            error: function () {
                showAlert('서버 통신 중 오류가 발생했습니다.', false);
            }
        });
    });

    // 3. 회원가입 비동기 제출 처리
    $('#registerForm').on('submit', function (e) {
        e.preventDefault();

        const userId = $('#regId').val().trim();
        const userPw = $('#regPw').val();

        $.ajax({
            url: '/register',
            type: 'POST',
            data: {
                userId: userId,
                userPw: userPw
            },
            success: function (res) {
                if (res.success) {
                    showAlert('회원가입 완료. 로그인해주세요.', true);
                    $('#loginId').val(userId);
                    $('#loginPw').val('');
                    setTimeout(function () {
                        switchAuthTab('login');
                    }, 800);
                } else {
                    showAlert(res.message || '회원가입에 실패했습니다.', false);
                }
            },
            error: function () {
                showAlert('서버 통신 중 오류가 발생했습니다.', false);
            }
        });
    });
});

// 4. 탭 전환 함수
function switchAuthTab(tab) {
    $('#authAlert').addClass('hidden');

    if (tab === 'login') {
        $('#loginForm').removeClass('hidden');
        $('#registerForm').addClass('hidden');
        $('#tab-login').addClass('active');
        $('#tab-register').removeClass('active');
    } else {
        $('#loginForm').addClass('hidden');
        $('#registerForm').removeClass('hidden');
        $('#tab-login').removeClass('active');
        $('#tab-register').addClass('active');
    }
}

// 5. 알림 메시지 노출 함수
function showAlert(message, isSuccess) {
    const $alert = $('#authAlert');
    $alert.text(message);
    $alert.attr('class', 'alert ' + (isSuccess ? 'alert-success' : 'alert-error'));
    $alert.removeClass('hidden');
}
