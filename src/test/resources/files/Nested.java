package com.easy.life;

public class Nested {
  // 6
  public void heavyNested() {
    if (true) {
      while (true) {
        for (int i = 0; i < 100; i++) {
          if (i > 50) {

          } else {
            if (true) {
              System.out.println("Ayyy");
            }
          }
        }
      }
    }
  }

  // 4
  public void heavyNested2(int a) {
    synchronized (this) {
      switch (a) {
        case 5:

          break;

        default:
          break;
      }
    }
  }
}
