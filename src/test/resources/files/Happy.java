package com.easy.life;

import java.util.ArrayList;
import java.util.List;
// unresolvable extrenal improt from "pseudo jar"
import com.unknown.data.type.UnknownType;
import com.unknown.data.type.UnknownTypeWithGenerics;

public class Happy {
  private UnknownType t;
  UnknownTypeWithGenerics<String, List<Integer>> t2;
  List<List<Integer>> lst;

  public <T extends Number> List<T> fromArrayToList(T[] a) {
    t =  new UnknownType();
    t2 = new  UnknownTypeWithGenerics<>();
    System.out.println("Done, yay!");
    return new ArrayList<>();
  }

}

// f3a8d244b2d3c52325941d09cdeb1b07b8b37815