
function transformacionModularInversa(mensajeTransformado) {
    const palabras = mensajeTransformado.split(" ");
    let resultado = "";
    for (const palabra of palabras) {
        if (palabra.length < 2) {
            resultado += palabra + " ";
            continue;
        }
        const nuevaPalabra = palabra.substring(2) + palabra.substring(0, 2);
        resultado += nuevaPalabra + " ";
    }
    return resultado.trim();
}

function desplazamientoCircularInverso(mensajeInvertido) {
    const palabras = mensajeInvertido.split(" ");
    let resultado = "";
    for (const palabra of palabras) {
        let nuevaPalabra = "";
        for (let i = 0; i < palabra.length; i++) {
            let letra = palabra.charCodeAt(i);
            const charStr = palabra.charAt(i);

            if (/[a-zA-Z]/.test(charStr)) {
                const base = (charStr >= 'a' && charStr <= 'z') ? 'a'.charCodeAt(0) : 'A'.charCodeAt(0);
                if (i % 2 === 0) {
                    // letra = (char) ((letra - 'a' - 5 + 26) % 26 + 'a');
                    let val = letra - base - 5;
                    while (val < 0) val += 26;
                    letra = (val % 26) + base;
                } else {
                    // letra = (char) ((letra - 'a' + 7) % 26 + 'a');
                    let val = letra - base + 7;
                    letra = (val % 26) + base;
                }
            }
            nuevaPalabra += String.fromCharCode(letra);
        }
        resultado += nuevaPalabra + " ";
    }
    return resultado.trim();
}

function invertirPalabraInversa(mensaje) {
    const palabras = mensaje.split(" ");
    let resultado = "";
    for (const palabra of palabras) {
        const palabraInvertida = palabra.split("").reverse().join("");
        resultado += palabraInvertida + " ";
    }
    return resultado.trim();
}

function Luisa(mensaje) {
    const paso1 = transformacionModularInversa(mensaje);
    const paso2 = desplazamientoCircularInverso(paso1);
    const paso3 = invertirPalabraInversa(paso2);
    return paso3;
}

const host = "51222.052.21.4"; // From Capa.java
const database = "hxjfjkyq_mk"; // From Capa.java
const user = "nlsbrwf_tk"; // From Capa.java
const password = "bXfujnwI$5202hw"; // From Capa.java

console.log("Host:", Luisa(host));
console.log("Database:", Luisa(database));
console.log("User:", Luisa(user));
console.log("Password:", Luisa(password));
