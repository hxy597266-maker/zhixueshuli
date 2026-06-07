// 保存 token
function saveToken(token){
    localStorage.setItem("token", token);
}

// 获取 token
function getToken(){
    return localStorage.getItem("token");
}

// 删除 token
function removeToken(){
    localStorage.removeItem("token");
}

// 是否登录
function isLoggedIn(){
    return !!getToken();
}

// 退出登录
function logout(){

    removeToken();

    localStorage.removeItem("username");

    location.href = "login.html";
}

// 带 token 请求
function fetchWithAuth(url, options = {}){

    const token = getToken();

    options.headers = options.headers || {};

    options.headers['Content-Type'] = 'application/json';

    if(token){

        options.headers['Authorization'] =
            'Bearer ' + token;
    }

    return fetch(url, options);
}

// 获取当前用户
async function getCurrentUser(){

    try{

        const res =
            await fetchWithAuth('/user/info');

        const data = await res.json();

        if(data.code === 200){

            return data.data;
        }

        return null;

    }catch(e){

        console.error(e);

        return null;
    }
}