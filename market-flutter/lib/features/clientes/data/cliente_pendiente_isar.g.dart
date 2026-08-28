// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cliente_pendiente_isar.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetClientePendienteIsarCollection on Isar {
  IsarCollection<ClientePendienteIsar> get clientePendienteIsars =>
      this.collection();
}

const ClientePendienteIsarSchema = CollectionSchema(
  name: r'ClientePendienteIsar',
  id: 5995156034749508888,
  properties: {
    r'clienteServidorId': PropertySchema(
      id: 0,
      name: r'clienteServidorId',
      type: IsarType.long,
    ),
    r'creadaEn': PropertySchema(
      id: 1,
      name: r'creadaEn',
      type: IsarType.dateTime,
    ),
    r'limiteCredito': PropertySchema(
      id: 2,
      name: r'limiteCredito',
      type: IsarType.string,
    ),
    r'mensajeError': PropertySchema(
      id: 3,
      name: r'mensajeError',
      type: IsarType.string,
    ),
    r'nit': PropertySchema(id: 4, name: r'nit', type: IsarType.string),
    r'nombre': PropertySchema(id: 5, name: r'nombre', type: IsarType.string),
    r'telefono': PropertySchema(
      id: 6,
      name: r'telefono',
      type: IsarType.string,
    ),
  },

  estimateSize: _clientePendienteIsarEstimateSize,
  serialize: _clientePendienteIsarSerialize,
  deserialize: _clientePendienteIsarDeserialize,
  deserializeProp: _clientePendienteIsarDeserializeProp,
  idName: r'id',
  indexes: {},
  links: {},
  embeddedSchemas: {},

  getId: _clientePendienteIsarGetId,
  getLinks: _clientePendienteIsarGetLinks,
  attach: _clientePendienteIsarAttach,
  version: '3.3.2',
);

int _clientePendienteIsarEstimateSize(
  ClientePendienteIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  {
    final value = object.limiteCredito;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  {
    final value = object.mensajeError;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  {
    final value = object.nit;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.nombre.length * 3;
  {
    final value = object.telefono;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  return bytesCount;
}

void _clientePendienteIsarSerialize(
  ClientePendienteIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeLong(offsets[0], object.clienteServidorId);
  writer.writeDateTime(offsets[1], object.creadaEn);
  writer.writeString(offsets[2], object.limiteCredito);
  writer.writeString(offsets[3], object.mensajeError);
  writer.writeString(offsets[4], object.nit);
  writer.writeString(offsets[5], object.nombre);
  writer.writeString(offsets[6], object.telefono);
}

ClientePendienteIsar _clientePendienteIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = ClientePendienteIsar();
  object.clienteServidorId = reader.readLongOrNull(offsets[0]);
  object.creadaEn = reader.readDateTime(offsets[1]);
  object.id = id;
  object.limiteCredito = reader.readStringOrNull(offsets[2]);
  object.mensajeError = reader.readStringOrNull(offsets[3]);
  object.nit = reader.readStringOrNull(offsets[4]);
  object.nombre = reader.readString(offsets[5]);
  object.telefono = reader.readStringOrNull(offsets[6]);
  return object;
}

P _clientePendienteIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readLongOrNull(offset)) as P;
    case 1:
      return (reader.readDateTime(offset)) as P;
    case 2:
      return (reader.readStringOrNull(offset)) as P;
    case 3:
      return (reader.readStringOrNull(offset)) as P;
    case 4:
      return (reader.readStringOrNull(offset)) as P;
    case 5:
      return (reader.readString(offset)) as P;
    case 6:
      return (reader.readStringOrNull(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _clientePendienteIsarGetId(ClientePendienteIsar object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _clientePendienteIsarGetLinks(
  ClientePendienteIsar object,
) {
  return [];
}

void _clientePendienteIsarAttach(
  IsarCollection<dynamic> col,
  Id id,
  ClientePendienteIsar object,
) {
  object.id = id;
}

extension ClientePendienteIsarQueryWhereSort
    on QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QWhere> {
  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhere>
  anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }
}

extension ClientePendienteIsarQueryWhere
    on QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QWhereClause> {
  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhereClause>
  idEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(lower: id, upper: id));
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhereClause>
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

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhereClause>
  idGreaterThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhereClause>
  idLessThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterWhereClause>
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

extension ClientePendienteIsarQueryFilter
    on
        QueryBuilder<
          ClientePendienteIsar,
          ClientePendienteIsar,
          QFilterCondition
        > {
  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'clienteServidorId'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'clienteServidorId'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdEqualTo(int? value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'clienteServidorId', value: value),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdGreaterThan(int? value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'clienteServidorId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdLessThan(int? value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'clienteServidorId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  clienteServidorIdBetween(
    int? lower,
    int? upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'clienteServidorId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'limiteCredito'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'limiteCredito'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'limiteCredito',
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
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'limiteCredito',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'limiteCredito',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'limiteCredito', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  limiteCreditoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'limiteCredito', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
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
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'nit'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'nit'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'nit',
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
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'nit',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'nit',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'nit', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nitIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'nit', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nombreIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'nombre', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  nombreIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'nombre', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'telefono'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'telefono'),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'telefono',
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
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'telefono',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'telefono',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'telefono', value: ''),
      );
    });
  }

  QueryBuilder<
    ClientePendienteIsar,
    ClientePendienteIsar,
    QAfterFilterCondition
  >
  telefonoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'telefono', value: ''),
      );
    });
  }
}

extension ClientePendienteIsarQueryObject
    on
        QueryBuilder<
          ClientePendienteIsar,
          ClientePendienteIsar,
          QFilterCondition
        > {}

extension ClientePendienteIsarQueryLinks
    on
        QueryBuilder<
          ClientePendienteIsar,
          ClientePendienteIsar,
          QFilterCondition
        > {}

extension ClientePendienteIsarQuerySortBy
    on QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QSortBy> {
  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByClienteServidorId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteServidorId', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByClienteServidorIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteServidorId', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByLimiteCredito() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'limiteCredito', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByLimiteCreditoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'limiteCredito', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByNit() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nit', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByNitDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nit', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByNombre() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByNombreDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByTelefono() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'telefono', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  sortByTelefonoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'telefono', Sort.desc);
    });
  }
}

extension ClientePendienteIsarQuerySortThenBy
    on QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QSortThenBy> {
  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByClienteServidorId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteServidorId', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByClienteServidorIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'clienteServidorId', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByCreadaEnDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'creadaEn', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByLimiteCredito() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'limiteCredito', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByLimiteCreditoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'limiteCredito', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByMensajeError() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByMensajeErrorDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'mensajeError', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByNit() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nit', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByNitDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nit', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByNombre() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByNombreDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.desc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByTelefono() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'telefono', Sort.asc);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QAfterSortBy>
  thenByTelefonoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'telefono', Sort.desc);
    });
  }
}

extension ClientePendienteIsarQueryWhereDistinct
    on QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct> {
  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByClienteServidorId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'clienteServidorId');
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByCreadaEn() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'creadaEn');
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByLimiteCredito({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(
        r'limiteCredito',
        caseSensitive: caseSensitive,
      );
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByMensajeError({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'mensajeError', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByNit({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'nit', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByNombre({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'nombre', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ClientePendienteIsar, ClientePendienteIsar, QDistinct>
  distinctByTelefono({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'telefono', caseSensitive: caseSensitive);
    });
  }
}

extension ClientePendienteIsarQueryProperty
    on
        QueryBuilder<
          ClientePendienteIsar,
          ClientePendienteIsar,
          QQueryProperty
        > {
  QueryBuilder<ClientePendienteIsar, int, QQueryOperations> idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<ClientePendienteIsar, int?, QQueryOperations>
  clienteServidorIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'clienteServidorId');
    });
  }

  QueryBuilder<ClientePendienteIsar, DateTime, QQueryOperations>
  creadaEnProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'creadaEn');
    });
  }

  QueryBuilder<ClientePendienteIsar, String?, QQueryOperations>
  limiteCreditoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'limiteCredito');
    });
  }

  QueryBuilder<ClientePendienteIsar, String?, QQueryOperations>
  mensajeErrorProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'mensajeError');
    });
  }

  QueryBuilder<ClientePendienteIsar, String?, QQueryOperations> nitProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'nit');
    });
  }

  QueryBuilder<ClientePendienteIsar, String, QQueryOperations>
  nombreProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'nombre');
    });
  }

  QueryBuilder<ClientePendienteIsar, String?, QQueryOperations>
  telefonoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'telefono');
    });
  }
}
