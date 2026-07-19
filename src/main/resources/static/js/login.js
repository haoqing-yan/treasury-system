document.addEventListener("DOMContentLoaded", async () => {
    const form = document.getElementById("loginForm");
    const username = document.getElementById("username");
    const password = document.getElementById("password");
    const error = document.getElementById("loginError");

    try {
        const response = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        const csrf = await response.json();
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = csrf.parameterName;
        input.value = csrf.token;
        form.appendChild(input);
    } catch (_) {
        error.textContent = "安全令牌初始化失败，请刷新页面重试。";
        error.classList.add("show");
    }

    if (new URLSearchParams(location.search).has("error")) {
        error.classList.add("show");
    }

    document.querySelectorAll(".demo-roles button").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".demo-roles button").forEach(item => item.classList.remove("selected"));
            button.classList.add("selected");
            username.value = button.dataset.user;
            password.value = button.dataset.password;
            error.classList.remove("show");
        });
    });

    document.getElementById("togglePassword").addEventListener("click", event => {
        const show = password.type === "password";
        password.type = show ? "text" : "password";
        event.currentTarget.textContent = show ? "隐藏" : "显示";
    });
});
