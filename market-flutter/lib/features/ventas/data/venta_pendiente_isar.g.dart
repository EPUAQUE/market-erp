// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'venta_pendiente_isar.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetVentaPendienteIsarCollection on Isar {
  IsarCollection<VentaPendienteIsar> get ventaPendienteIsars =>
      this.collection();
}

const VentaPendienteIsarSchema = CollectionSchema(
  name: r'VentaPendienteIsar',
  id: -4889342510325346360,
  properties: {
    r'clienteId': PropertySchema(
      id: 0,
      name: r'clienteId',
      type: IsarType.long,
    ),
    r'correlationId': PropertySchema(
      id: 1,
      name: r'correlationId',
      type: IsarType.string,
    ),
    r'creadaEn': PropertySchema(
      id: 2,
      name: r'creadaEn',
      type: IsarType.dateTime,
    ),
    r'lineas': PropertySchema(
      id: 3,
      name: r'lineas',
      type: IsarType.objectList,

      target: r'LineaCarritoIsar',
    ),
    r'mensajeError': PropertySchema(
      id: 4,
      name: r'mensajeError',
      type: IsarType.string,
    ),
    r'metodoPago': PropertySchema(
      id: 5,
      name: r'metodoPago',
      type: IsarType.string,
    ),
    r'montoACobrar': PropertySchema(
      id: 6,
      name: r'montoACobrar',
      type: IsarType.string,
    ),
    r'tiendaId': PropertySchema(id: 7, name: r'tiendaId', type: IsarType.long),
  },

  estimateSize: _ventaPendienteIsarEstimateSize,
  serialize: _ventaPendienteIsarSerialize,
  deserialize: _ventaPendienteIsarDeserialize,
  deserializeProp: _ventaPendienteIsarDeserializeProp,
  idName: r'id',
  indexes: {},
  links: {},
  embeddedSchemas: {r'LineaCarritoIsar': LineaCarritoIsarSchema},

  getId: _ventaPendienteIsarGetId,
  getLinks: _ventaPendienteIsarGetLinks,
  attach: _ventaPendienteIsarAttach,
  version: '3.3.2',
);

int _ventaPendienteIsarEstimateSize(
  VentaPendienteIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  bytesCount += 3 + object.correlationId.length * 3;
  bytesCount += 3 + object.lineas.length * 3;
  {
    final offsets = allOffsets[LineaCarritoIsar]!;
    for (var i = 0; i < object.lineas.length; i++) {
      final value = object.lineas[i];
      bytesCount += LineaCarritoIsarSchema.estimateSize(
        value,
        offsets,
        allOffsets,
      );
    }
  }
  {
    final value = object.mensajeError;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.metodoPago.length * 3;
  {
    final value = object.montoACobrar;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  return bytesCount;
}

void _ventaPendienteIsarSerialize(
  VentaPendienteIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeLong(offsets[0], object.clienteId);
  writer.writeString(offsets[1], object.correlationId);
  writer.writeDateTime(offsets[2], object.creadaEn);
  writer.writeObjectList<LineaCarritoIsar>(
    offsets[3],
    allOffsets,
    LineaCarritoIsarSchema.serialize,
    object.lineas,
  );
  writer.writeString(offsets[4], object.mensajeError);
  writer.writeString(offsets[5], object.metodoPago);
  writer.writeString(offsets[6], object.montoACobrar);
  writer.writeLong(offsets[7], object.tiendaId);
}

VentaPendienteIsar _ventaPendienteIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = VentaPendienteIsar();
  object.clienteId = reader.readLong(offsets[0]);
  object.correlationId = reader.readString(offsets[1]);
  object.creadaEn = reader.readDateTime(offsets[2]);
  object.id = id;
  object.lineas =
      reader.readObjectList<LineaCarritoIsar>(
        offsets[3],
        LineaCarritoIsarSchema.deserialize,
        allOffsets,
        LineaCarritoIsar(),
      ) ??
      [];
  object.mensajeError = reader.readStringOrNull(offsets[4]);
  object.metodoPago = reader.readString(offsets[5]);
  object.montoACobrar = reader.readStringOrNull(offsets[6]);
  object.tiendaId = reader.readLong(offsets[7]);
  return object;
}

P _ventaPendienteIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readLong(offset)) as P;
    case 1:
      return (reader.readString(offset)) as P;
    case 2:
      return (reader.readDateTime(offset)) as P;
    case 3:
      return (reader.readObjectList<LineaCarritoIsar>(
                offset,
                LineaCarritoIsarSchema.deserialize,
                allOffsets,
                LineaCarritoIsar(),
              ) ??
              [])
          as P;
    case 4:
      return (reader.readStringOrNull(offset)) as P;
    case 5:
      return (reader.readString(offset)) as P;
    case 6:
      return (reader.readStringOrNull(offset)) as P;
    case 7:
      return (reader.readLong(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _ventaPendienteIsarGetId(VentaPendienteIsar object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _ventaPendienteIsarGetLinks(
  VentaPendienteIsar object,
) {
  return [];
}

void _ventaPendienteIsarAttach(
  IsarCollection<dynamic> col,
  Id id,
  VentaPendienteIsar object,
) {
  object.id = id;
}

extension VentaPendienteIsarQueryWhereSort
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QWhere> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhere> anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }
}

extension VentaPendienteIsarQueryWhere
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QWhereClause> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhereClause>
  idEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(lower: id, upper: id));
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhereClause>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhereClause>
  idGreaterThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhereClause>
  idLessThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterWhereClause>
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

extension VentaPendienteIsarQueryFilter
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QFilterCondition> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  clienteIdEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'clienteId', value: value),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  clienteIdGreaterThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'clienteId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  clienteIdLessThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'clienteId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  clienteIdBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'clienteId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'correlationId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'correlationId',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'correlationId',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'correlationId', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  correlationIdIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'correlationId', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  creadaEnEqualTo(DateTime value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'creadaEn', value: value),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  idEqualTo(Id value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'id', value: value),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasLengthEqualTo(int length) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(r'lineas', length, true, length, true);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(r'lineas', 0, true, 0, true);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(r'lineas', 0, false, 999999, true);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasLengthLessThan(int length, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(r'lineas', 0, true, length, include);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasLengthGreaterThan(int length, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(r'lineas', length, include, 999999, true);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasLengthBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'lineas',
        lower,
        includeLower,
        upper,
        includeUpper,
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  mensajeErrorIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'mensajeError'),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  mensajeErrorIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'mensajeError'),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  mensajeErrorIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'mensajeError', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  mensajeErrorIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'mensajeError', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'metodoPago',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'metodoPago',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'metodoPago',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'metodoPago', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  metodoPagoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'metodoPago', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'montoACobrar'),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'montoACobrar'),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'montoACobrar',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'montoACobrar',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'montoACobrar',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'montoACobrar', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  montoACobrarIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'montoACobrar', value: ''),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  tiendaIdEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'tiendaId', value: value),
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
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
}

extension VentaPendienteIsarQueryObject
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QFilterCondition> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterFilterCondition>
  lineasElement(FilterQuery<LineaCarritoIsar> q) {
    return QueryBuilder.apply(this, (query) {
      return query.object(q, r'lineas');
    });
  }
}

extension VentaPendienteIsarQueryLinks
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QFilterCondition> {}

extension VentaPendienteIsarQuerySortBy
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QSortBy> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByClienteId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByClienteIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteId', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByCorrelationId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'correlationId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByCorrelationIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'correlationId', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMetodoPago() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'metodoPago', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMetodoPagoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'metodoPago', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMontoACobrar() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'montoACobrar', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByMontoACobrarDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'montoACobrar', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  sortByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }
}

extension VentaPendienteIsarQuerySortThenBy
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QSortThenBy> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByClienteId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByClienteIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteId', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByCorrelationId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'correlationId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByCorrelationIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'correlationId', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMetodoPago() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'metodoPago', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMetodoPagoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'metodoPago', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMontoACobrar() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'montoACobrar', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByMontoACobrarDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'montoACobrar', Sort.desc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QAfterSortBy>
  thenByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }
}

extension VentaPendienteIsarQueryWhereDistinct
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct> {
  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByClienteId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'clienteId');
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByCorrelationId({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(
        r'correlationId',
        caseSensitive: caseSensitive,
      );
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'creadaEn');
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByMensajeError({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'mensajeError', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByMetodoPago({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'metodoPago', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByMontoACobrar({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'montoACobrar', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QDistinct>
  distinctByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'tiendaId');
    });
  }
}

extension VentaPendienteIsarQueryProperty
    on QueryBuilder<VentaPendienteIsar, VentaPendienteIsar, QQueryProperty> {
  QueryBuilder<VentaPendienteIsar, int, QQueryOperations> idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<VentaPendienteIsar, int, QQueryOperations> clienteIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'clienteId');
    });
  }

  QueryBuilder<VentaPendienteIsar, String, QQueryOperations>
  correlationIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'correlationId');
    });
  }

  QueryBuilder<VentaPendienteIsar, DateTime, QQueryOperations>
  creadaEnProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'creadaEn');
    });
  }

  QueryBuilder<VentaPendienteIsar, List<LineaCarritoIsar>, QQueryOperations>
  lineasProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'lineas');
    });
  }

  QueryBuilder<VentaPendienteIsar, String?, QQueryOperations>
  mensajeErrorProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'mensajeError');
    });
  }

  QueryBuilder<VentaPendienteIsar, String, QQueryOperations>
  metodoPagoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'metodoPago');
    });
  }

  QueryBuilder<VentaPendienteIsar, String?, QQueryOperations>
  montoACobrarProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'montoACobrar');
    });
  }

  QueryBuilder<VentaPendienteIsar, int, QQueryOperations> tiendaIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'tiendaId');
    });
  }
}

// **************************************************************************
// IsarEmbeddedGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

const LineaCarritoIsarSchema = Schema(
  name: r'LineaCarritoIsar',
  id: -808006140888726066,
  properties: {
    r'cantidad': PropertySchema(
      id: 0,
      name: r'cantidad',
      type: IsarType.string,
    ),
    r'nombre': PropertySchema(id: 1, name: r'nombre', type: IsarType.string),
    r'precioUnitario': PropertySchema(
      id: 2,
      name: r'precioUnitario',
      type: IsarType.string,
    ),
    r'productoId': PropertySchema(
      id: 3,
      name: r'productoId',
      type: IsarType.long,
    ),
  },

  estimateSize: _lineaCarritoIsarEstimateSize,
  serialize: _lineaCarritoIsarSerialize,
  deserialize: _lineaCarritoIsarDeserialize,
  deserializeProp: _lineaCarritoIsarDeserializeProp,
);

int _lineaCarritoIsarEstimateSize(
  LineaCarritoIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  bytesCount += 3 + object.cantidad.length * 3;
  bytesCount += 3 + object.nombre.length * 3;
  bytesCount += 3 + object.precioUnitario.length * 3;
  return bytesCount;
}

void _lineaCarritoIsarSerialize(
  LineaCarritoIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeString(offsets[0], object.cantidad);
  writer.writeString(offsets[1], object.nombre);
  writer.writeString(offsets[2], object.precioUnitario);
  writer.writeLong(offsets[3], object.productoId);
}

LineaCarritoIsar _lineaCarritoIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = LineaCarritoIsar();
  object.cantidad = reader.readString(offsets[0]);
  object.nombre = reader.readString(offsets[1]);
  object.precioUnitario = reader.readString(offsets[2]);
  object.productoId = reader.readLong(offsets[3]);
  return object;
}

P _lineaCarritoIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readString(offset)) as P;
    case 1:
      return (reader.readString(offset)) as P;
    case 2:
      return (reader.readString(offset)) as P;
    case 3:
      return (reader.readLong(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

extension LineaCarritoIsarQueryFilter
    on QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QFilterCondition> {
  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'cantidad',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'cantidad',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'cantidad',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'cantidad', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  cantidadIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'cantidad', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'nombre',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'nombre',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'nombre',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'nombre', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  nombreIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'nombre', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'precioUnitario',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'precioUnitario',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'precioUnitario',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'precioUnitario', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  precioUnitarioIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'precioUnitario', value: ''),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  productoIdEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'productoId', value: value),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  productoIdGreaterThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'productoId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  productoIdLessThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'productoId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QAfterFilterCondition>
  productoIdBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'productoId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }
}

extension LineaCarritoIsarQueryObject
    on QueryBuilder<LineaCarritoIsar, LineaCarritoIsar, QFilterCondition> {}
