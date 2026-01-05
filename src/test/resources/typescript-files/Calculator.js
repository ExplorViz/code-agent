// Simple JavaScript file for testing

class Calculator {
    constructor() {
        this.result = 0;
    }

    add(a, b) {
        return a + b;
    }

    subtract(a, b) {
        return a - b;
    }

    multiply(a, b) {
        return a * b;
    }

    divide(a, b) {
        if (b === 0) {
            throw new Error('Division by zero');
        }
        return a / b;
    }
}

function calculate(operation, x, y) {
    const calc = new Calculator();
    return calc[operation](x, y);
}

module.exports = { Calculator, calculate };

