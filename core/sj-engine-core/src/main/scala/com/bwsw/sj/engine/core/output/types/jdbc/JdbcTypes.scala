package com.bwsw.sj.engine.core.output.types.jdbc

import java.sql.PreparedStatement

import org.apache.commons.lang3.StringEscapeUtils
import com.bwsw.sj.engine.core.output.{IncompatibleTypeException, BasicType}

/**
  * Created by Ivan Kudryavtsev on 03.03.2017.
  */

abstract class JdbcField[T](name: String, default: T) extends BasicType[(PreparedStatement, Int) => Unit, T](name, default)

class DecimalField(name: String, default: java.math.BigDecimal = java.math.BigDecimal.ZERO) extends JdbcField[java.math.BigDecimal](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setBigDecimal(idx, null)
    case i: java.math.BigDecimal => (ps: PreparedStatement, idx: Int) => ps.setBigDecimal(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.math.BigDecimal.")
  }
}

class IntegerField(name: String, default: java.lang.Integer = 0) extends JdbcField[java.lang.Integer](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setInt(idx, null.asInstanceOf[java.lang.Integer])
    case i: java.lang.Integer => (ps: PreparedStatement, idx: Int) => ps.setInt(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Integer.")
  }
}

class LongField(name: String, default: java.lang.Long = 0L) extends JdbcField[java.lang.Long](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setLong(idx, null.asInstanceOf[java.lang.Long])
    case i: java.lang.Long => (ps: PreparedStatement, idx: Int) => ps.setLong(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Long.")
  }
}

class FloatField(name: String, default: java.lang.Float = 0.0f) extends JdbcField[java.lang.Float](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setFloat(idx, null.asInstanceOf[java.lang.Float])
    case i: java.lang.Float => (ps: PreparedStatement, idx: Int) => ps.setFloat(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Float.")
  }
}
//
class DoubleField(name: String, default: java.lang.Double = 0.0) extends JdbcField[java.lang.Double](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setDouble(idx, null.asInstanceOf[java.lang.Double])
    case i: java.lang.Double => (ps: PreparedStatement, idx: Int) => ps.setDouble(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Double.")
  }
}
//
class ByteField(name: String, default: java.lang.Byte = 0.toByte) extends JdbcField[java.lang.Byte](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setByte(idx, null.asInstanceOf[java.lang.Byte])
    case i: java.lang.Byte => (ps: PreparedStatement, idx: Int) => ps.setByte(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Byte.")
  }
}
//
class CharField(name: String, default: java.lang.Character = 0.toChar) extends JdbcField[java.lang.Character](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null.asInstanceOf[java.lang.String])
    case i: java.lang.Character => (ps: PreparedStatement, idx: Int) => ps.setString(idx, i.toString)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Character.")
  }
}

class ShortField(name: String, default: java.lang.Short = 0.toShort) extends JdbcField[java.lang.Short](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setShort(idx, null.asInstanceOf[java.lang.Short])
    case i: java.lang.Short => (ps: PreparedStatement, idx: Int) => ps.setShort(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Short.")
  }
}

class BooleanField(name: String, default: java.lang.Boolean = true) extends JdbcField[java.lang.Boolean](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setBoolean(idx, null.asInstanceOf[java.lang.Boolean])
    case i: java.lang.Boolean => (ps: PreparedStatement, idx: Int) => ps.setBoolean(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Boolean.")
  }
}

class DateField(name: String, default: java.sql.Date = new java.sql.Date(0L)) extends JdbcField[java.sql.Date](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setDate(idx, null.asInstanceOf[java.sql.Date])
    case i: java.sql.Date => (ps: PreparedStatement, idx: Int) => ps.setDate(idx, i)
    case i: java.lang.Long => (ps: PreparedStatement, idx: Int) => ps.setDate(idx, new java.sql.Date(i))
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.sql.Date.")
  }
}

class BinaryField(name: String, default: Array[Byte] = new Array[Byte](0)) extends JdbcField[Array[Byte]](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setBytes(idx, null.asInstanceOf[Array[Byte]])
    case i: Array[Byte] => (ps: PreparedStatement, idx: Int) => ps.setBytes(idx, i)
    case _ => throw new IncompatibleTypeException(s"Field '$name' has incompatible type ${fieldValue.getClass.getName}. Must be java.lang.Array[Byte].")
  }
}

// TODO check correct data
class JavaStringField(name: String, default: java.lang.String = "") extends JdbcField[java.lang.String](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null.asInstanceOf[java.lang.String])
    case i: java.lang.String => (ps: PreparedStatement, idx: Int) => ps.setString(idx, StringEscapeUtils.escapeJava(i))
    case _ => (ps: PreparedStatement, idx: Int) => ps.setString(idx, s"${StringEscapeUtils.escapeJava(fieldValue.toString)}")
  }
}

// TODO check correct data
class HTMLStringField(name: String, default: java.lang.String = "") extends JdbcField[java.lang.String](name, default) {
  override def transform(fieldValue: Any): (PreparedStatement, Int) => Unit = fieldValue match {
    case null => (ps: PreparedStatement, idx: Int) => ps.setString(idx, null.asInstanceOf[java.lang.String])
    case i: java.lang.String => (ps: PreparedStatement, idx: Int) => ps.setString(idx, StringEscapeUtils.escapeHtml4(i))
    case _ => (ps: PreparedStatement, idx: Int) => ps.setString(idx, StringEscapeUtils.escapeHtml4(fieldValue.toString))
  }
}