// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'movimiento_caja_pendiente_isar.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetMovimientoCajaPendienteIsarCollection on Isar {
  IsarCollection<MovimientoCajaPendienteIsar>
  get movimientoCajaPendienteIsars => this.collection();
}

const MovimientoCajaPendienteIsarSchema = CollectionSchema(
  name: r'MovimientoCajaPendienteIsar',
  id: 749856581701936026,
  properties: {
    r'concepto': PropertySchema(
      id: 0,
      name: r'concepto',
      type: IsarType.string,
    ),
    r'creadaEn': PropertySchema(
      id: 1,
      name: r'creadaEn',
      type: IsarType.dateTime,
    ),
    r'mensajeError': PropertySchema(
      id: 2,
      name: r'mensajeError',
      type: IsarType.string,
    ),
    r'monto': PropertySchema(id: 3, name: r'monto', type: IsarType.string),
    r'tiendaId': PropertySchema(id: 4, name: r'tiendaId', type: IsarType.long),
    r'tipo': PropertySchema(id: 5, name: r'tipo', type: IsarType.string),
  },

  estimateSize: _movimientoCajaPendienteIsarEstimateSize,
  serialize: _movimientoCajaPendienteIsarSerialize,
  deserialize: _movimientoCajaPendienteIsarDeserialize,
  deserializeProp: _movimientoCajaPendienteIsarDeserializeProp,
  idName: r'id',
  indexes: {},
  links: {},
  embeddedSchemas: {},

  getId: _movimientoCajaPendienteIsarGetId,
  getLinks: _movimientoCajaPendienteIsarGetLinks,
  attach: _movimientoCajaPendienteIsarAttach,
  version: '3.3.2',
);

int _movimientoCajaPendienteIsarEstimateSize(
  MovimientoCajaPendienteIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  bytesCount += 3 + object.concepto.length * 3;
  {
    final value = object.mensajeError;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.monto.length * 3;
  bytesCount += 3 + object.tipo.length * 3;
  return bytesCount;
}

void _movimientoCajaPendienteIsarSerialize(
  MovimientoCajaPendienteIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeString(offsets[0], object.concepto);
  writer.writeDateTime(offsets[1], object.creadaEn);
  writer.writeString(offsets[2], object.mensajeError);
  writer.writeString(offsets[3], object.monto);
  writer.writeLong(offsets[4], object.tiendaId);
  writer.writeString(offsets[5], object.tipo);
}

MovimientoCajaPendienteIsar _movimientoCajaPendienteIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = MovimientoCajaPendienteIsar();
  object.concepto = reader.readString(offsets[0]);
  object.creadaEn = reader.readDateTime(offsets[1]);
  object.id = id;
  object.mensajeError = reader.readStringOrNull(offsets[2]);
  object.monto = reader.readString(offsets[3]);
  object.tiendaId = reader.readLong(offsets[4]);
  object.tipo = reader.readString(offsets[5]);
  return object;
}

P _movimientoCajaPendienteIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readString(offset)) as P;
    case 1:
      return (reader.readDateTime(offset)) as P;
    case 2:
      return (reader.readStringOrNull(offset)) as P;
    case 3:
      return (reader.readString(offset)) as P;
    case 4:
      return (reader.readLong(offset)) as P;
    case 5:
      return (reader.readString(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _movimientoCajaPendienteIsarGetId(MovimientoCajaPendienteIsar object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _movimientoCajaPendienteIsarGetLinks(
  MovimientoCajaPendienteIsar object,
) {
  return [];
}

void _movimientoCajaPendienteIsarAttach(
  IsarCollection<dynamic> col,
  Id id,
  MovimientoCajaPendienteIsar object,
) {
  object.id = id;
}

extension MovimientoCajaPendienteIsarQueryWhereSort
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QWhere
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhere
  >
  anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }
}

extension MovimientoCajaPendienteIsarQueryWhere
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QWhereClause
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhereClause
  >
  idEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(lower: id, upper: id));
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhereClause
  >
  idNotEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      if (query.whereSort == Sort.asc) {
        return query
            .addWhereClause(
              IdWhereClause.lessThan(upper: id, includeUpper: false),
            )
            .addWhereClause(
              IdWhereClause.greaterThan(lower: id, includeLower: false),
            );
      } else {
        return query
            .addWhereClause(
              IdWhereClause.greaterThan(lower: id, includeLower: false),
            )
            .addWhereClause(
              IdWhereClause.lessThan(upper: id, includeUpper: false),
            );
      }
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhereClause
  >
  idGreaterThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhereClause
  >
  idLessThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterWhereClause
  >
  idBetween(
    Id lowerId,
    Id upperId, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.between(
          lower: lowerId,
          includeLower: includeLower,
          upper: upperId,
          includeUpper: includeUpper,
        ),
      );
    });
  }
}

extension MovimientoCajaPendienteIsarQueryFilter
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QFilterCondition
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'concepto',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'concepto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'concepto',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'concepto', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  conceptoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'concepto', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  creadaEnEqualTo(DateTime value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'creadaEn', value: value),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  creadaEnGreaterThan(DateTime value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'creadaEn',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  creadaEnLessThan(DateTime value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'creadaEn',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  creadaEnBetween(
    DateTime lower,
    DateTime upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'creadaEn',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  idEqualTo(Id value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'id', value: value),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  idGreaterThan(Id value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'id',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  idLessThan(Id value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'id',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  idBetween(
    Id lower,
    Id upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'id',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'mensajeError'),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'mensajeError'),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'mensajeError',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'mensajeError',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'mensajeError',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'mensajeError', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  mensajeErrorIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'mensajeError', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'monto',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'monto',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'monto',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'monto', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  montoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'monto', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tiendaIdEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'tiendaId', value: value),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tiendaIdGreaterThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'tiendaId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tiendaIdLessThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'tiendaId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tiendaIdBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'tiendaId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'tipo',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'tipo',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'tipo',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'tipo', value: ''),
      );
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterFilterCondition
  >
  tipoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'tipo', value: ''),
      );
    });
  }
}

extension MovimientoCajaPendienteIsarQueryObject
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QFilterCondition
        > {}

extension MovimientoCajaPendienteIsarQueryLinks
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QFilterCondition
        > {}

extension MovimientoCajaPendienteIsarQuerySortBy
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QSortBy
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByConcepto() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'concepto', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByConceptoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'concepto', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByMonto() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'monto', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByMontoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'monto', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByTipo() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tipo', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  sortByTipoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tipo', Sort.desc);
    });
  }
}

extension MovimientoCajaPendienteIsarQuerySortThenBy
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QSortThenBy
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByConcepto() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'concepto', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByConceptoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'concepto', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByMonto() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'monto', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByMontoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'monto', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByTipo() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tipo', Sort.asc);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QAfterSortBy
  >
  thenByTipoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tipo', Sort.desc);
    });
  }
}

extension MovimientoCajaPendienteIsarQueryWhereDistinct
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QDistinct
        > {
  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByConcepto({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'concepto', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'creadaEn');
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByMensajeError({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'mensajeError', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByMonto({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'monto', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'tiendaId');
    });
  }

  QueryBuilder<
    MovimientoCajaPendienteIsar,
    MovimientoCajaPendienteIsar,
    QDistinct
  >
  distinctByTipo({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'tipo', caseSensitive: caseSensitive);
    });
  }
}

extension MovimientoCajaPendienteIsarQueryProperty
    on
        QueryBuilder<
          MovimientoCajaPendienteIsar,
          MovimientoCajaPendienteIsar,
          QQueryProperty
        > {
  QueryBuilder<MovimientoCajaPendienteIsar, int, QQueryOperations>
  idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, String, QQueryOperations>
  conceptoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'concepto');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, DateTime, QQueryOperations>
  creadaEnProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'creadaEn');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, String?, QQueryOperations>
  mensajeErrorProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'mensajeError');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, String, QQueryOperations>
  montoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'monto');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, int, QQueryOperations>
  tiendaIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'tiendaId');
    });
  }

  QueryBuilder<MovimientoCajaPendienteIsar, String, QQueryOperations>
  tipoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'tipo');
    });
  }
}
