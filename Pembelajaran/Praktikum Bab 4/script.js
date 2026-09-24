let display = document.getElementById("display");

function masukkan(nilai) {
    display.value += nilai;
}

function clearDisplay() {
    display.value = "";
}

function hapus() {
    display.value = display.value.slice(0, -1);
}

function hitung() {
    try {
        display.value = eval(display.value);
    } catch {
        display.value = "Error";
    }
}