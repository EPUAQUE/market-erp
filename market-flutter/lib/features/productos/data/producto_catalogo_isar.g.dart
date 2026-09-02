// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'producto_catalogo_isar.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetProductoCatalogoIsarCollection on Isar {
  IsarCollection<ProductoCatalogoIsar> get productoCatalogoIsars =>
      this.collection();
}

const ProductoCatalogoIsarSchema = CollectionSchema(
  name: r'ProductoCatalogoIsar',
  id: 2131202271008899467,
  properties: {
    r'categoriaId': PropertySchema(
      id: 0,
      name: r'categoriaId',
      type: IsarType.long,
    ),
    r'codigoBarras': PropertySchema(
      id: 1,
      name: r'codigoBarras',
      type: IsarType.string,
    ),
    r'codigoInterno': PropertySchema(
      id: 2,
      name: r'codigoInterno',
      type: IsarType.string,
    ),
    r'descripcionCorta': PropertySchema(
      id: 3,
      name: r'descripcionCorta',
      type: IsarType.string,
    ),
    r'existenciaActual': PropertySchema(
      id: 4,
      name: r'existenciaActual',
      type: IsarType.string,
    ),
    r'imagenUrl': PropertySchema(
      id: 5,
      name: r'imagenUrl',
      type: IsarType.string,
    ),
    r'nombre': PropertySchema(id: 6, name: r'nombre', type: IsarType.string),
    r'permitirVenta': PropertySchema(
      id: 7,
      name: r'permitirVenta',
      type: IsarType.bool,
    ),
    r'precioVenta': PropertySchema(
      id: 8,
      name: r'precioVenta',
      type: IsarType.string,
    ),
    r'productoId': PropertySchema(
      id: 9,
      name: r'productoId',
      type: IsarType.long,
    ),
    r'tiendaId': PropertySchema(id: 10, name: r'tiendaId', type: IsarType.long),
  },

  estimateSize: _productoCatalogoIsarEstimateSize,
  serialize: _productoCatalogoIsarSerialize,
  deserialize: _productoCatalogoIsarDeserialize,
  deserializeProp: _productoCatalogoIsarDeserializeProp,
  idName: r'id',
  indexes: {},
  links: {},
  embeddedSchemas: {},

  getId: _productoCatalogoIsarGetId,
  getLinks: _productoCatalogoIsarGetLinks,
  attach: _productoCatalogoIsarAttach,
  version: '3.3.2',
);

int _productoCatalogoIsarEstimateSize(
  ProductoCatalogoIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  {
    final value = object.codigoBarras;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.codigoInterno.length * 3;
  {
    final value = object.descripcionCorta;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.existenciaActual.length * 3;
  {
    final value = object.imagenUrl;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.nombre.length * 3;
  bytesCount += 3 + object.precioVenta.length * 3;
  return bytesCount;
}

void _productoCatalogoIsarSerialize(
  ProductoCatalogoIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeLong(offsets[0], object.categoriaId);
  writer.writeString(offsets[1], object.codigoBarras);
  writer.writeString(offsets[2], object.codigoInterno);
  writer.writeString(offsets[3], object.descripcionCorta);
  writer.writeString(offsets[4], object.existenciaActual);
  writer.writeString(offsets[5], object.imagenUrl);
  writer.writeString(offsets[6], object.nombre);
  writer.writeBool(offsets[7], object.permitirVenta);
  writer.writeString(offsets[8], object.precioVenta);
  writer.writeLong(offsets[9], object.productoId);
  writer.writeLong(offsets[10], object.tiendaId);
}

ProductoCatalogoIsar _productoCatalogoIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = ProductoCatalogoIsar();
  object.categoriaId = reader.readLongOrNull(offsets[0]);
  object.codigoBarras = reader.readStringOrNull(offsets[1]);
  object.codigoInterno = reader.readString(offsets[2]);
  object.descripcionCorta = reader.readStringOrNull(offsets[3]);
  object.existenciaActual = reader.readString(offsets[4]);
  object.imagenUrl = reader.readStringOrNull(offsets[5]);
  object.nombre = reader.readString(offsets[6]);
  object.permitirVenta = reader.readBool(offsets[7]);
  object.precioVenta = reader.readString(offsets[8]);
  object.productoId = reader.readLong(offsets[9]);
  object.tiendaId = reader.readLong(offsets[10]);
  return object;
}

P _productoCatalogoIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readLongOrNull(offset)) as P;
    case 1:
      return (reader.readStringOrNull(offset)) as P;
    case 2:
      return (reader.readString(offset)) as P;
    case 3:
      return (reader.readStringOrNull(offset)) as P;
    case 4:
      return (reader.readString(offset)) as P;
    case 5:
      return (reader.readStringOrNull(offset)) as P;
    case 6:
      return (reader.readString(offset)) as P;
    case 7:
      return (reader.readBool(offset)) as P;
    case 8:
      return (reader.readString(offset)) as P;
    case 9:
      return (reader.readLong(offset)) as P;
    case 10:
      return (reader.readLong(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _productoCatalogoIsarGetId(ProductoCatalogoIsar object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _productoCatalogoIsarGetLinks(
  ProductoCatalogoIsar object,
) {
  return [];
}

void _productoCatalogoIsarAttach(
  IsarCollection<dynamic> col,
  Id id,
  ProductoCatalogoIsar object,
) {}

extension ProductoCatalogoIsarQueryWhereSort
    on QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QWhere> {
  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhere>
  anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }
}

extension ProductoCatalogoIsarQueryWhere
    on QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QWhereClause> {
  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhereClause>
  idEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(lower: id, upper: id));
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhereClause>
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

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhereClause>
  idGreaterThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhereClause>
  idLessThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterWhereClause>
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

extension ProductoCatalogoIsarQueryFilter
    on
        QueryBuilder<
          ProductoCatalogoIsar,
          ProductoCatalogoIsar,
          QFilterCondition
        > {
  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'categoriaId'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'categoriaId'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdEqualTo(int? value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'categoriaId', value: value),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdGreaterThan(int? value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'categoriaId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdLessThan(int? value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'categoriaId',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  categoriaIdBetween(
    int? lower,
    int? upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'categoriaId',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'codigoBarras'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'codigoBarras'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'codigoBarras',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'codigoBarras',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'codigoBarras',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'codigoBarras', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoBarrasIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'codigoBarras', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'codigoInterno',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'codigoInterno',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'codigoInterno',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'codigoInterno', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  codigoInternoIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'codigoInterno', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'descripcionCorta'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'descripcionCorta'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'descripcionCorta',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'descripcionCorta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'descripcionCorta',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'descripcionCorta', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  descripcionCortaIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'descripcionCorta', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'existenciaActual',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'existenciaActual',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'existenciaActual',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'existenciaActual', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  existenciaActualIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'existenciaActual', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNull(property: r'imagenUrl'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        const FilterCondition.isNotNull(property: r'imagenUrl'),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlEqualTo(String? value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'imagenUrl',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'imagenUrl',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'imagenUrl',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'imagenUrl', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  imagenUrlIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'imagenUrl', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  permitirVentaEqualTo(bool value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'permitirVenta', value: value),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaEqualTo(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'precioVenta',
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaStartsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.startsWith(
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaEndsWith(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.endsWith(
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.contains(
          property: r'precioVenta',
          value: value,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.matches(
          property: r'precioVenta',
          wildcard: pattern,
          caseSensitive: caseSensitive,
        ),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'precioVenta', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  precioVentaIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(property: r'precioVenta', value: ''),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
  productoIdEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'productoId', value: value),
      );
    });
  }

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
    QAfterFilterCondition
  >
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

  QueryBuilder<
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
    ProductoCatalogoIsar,
    ProductoCatalogoIsar,
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
}

extension ProductoCatalogoIsarQueryObject
    on
        QueryBuilder<
          ProductoCatalogoIsar,
          ProductoCatalogoIsar,
          QFilterCondition
        > {}

extension ProductoCatalogoIsarQueryLinks
    on
        QueryBuilder<
          ProductoCatalogoIsar,
          ProductoCatalogoIsar,
          QFilterCondition
        > {}

extension ProductoCatalogoIsarQuerySortBy
    on QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QSortBy> {
  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCategoriaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'categoriaId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCategoriaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'categoriaId', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCodigoBarras() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoBarras', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCodigoBarrasDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoBarras', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCodigoInterno() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoInterno', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByCodigoInternoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoInterno', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByDescripcionCorta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'descripcionCorta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByDescripcionCortaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'descripcionCorta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByExistenciaActual() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'existenciaActual', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByExistenciaActualDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'existenciaActual', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByImagenUrl() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'imagenUrl', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByImagenUrlDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'imagenUrl', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByNombre() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByNombreDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByPermitirVenta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'permitirVenta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByPermitirVentaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'permitirVenta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByPrecioVenta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'precioVenta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByPrecioVentaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'precioVenta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByProductoId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'productoId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByProductoIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'productoId', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  sortByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }
}

extension ProductoCatalogoIsarQuerySortThenBy
    on QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QSortThenBy> {
  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCategoriaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'categoriaId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCategoriaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'categoriaId', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCodigoBarras() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoBarras', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCodigoBarrasDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoBarras', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCodigoInterno() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoInterno', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByCodigoInternoDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'codigoInterno', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByDescripcionCorta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'descripcionCorta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByDescripcionCortaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'descripcionCorta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByExistenciaActual() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'existenciaActual', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByExistenciaActualDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'existenciaActual', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByImagenUrl() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'imagenUrl', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByImagenUrlDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'imagenUrl', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByNombre() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByNombreDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'nombre', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByPermitirVenta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'permitirVenta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByPermitirVentaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'permitirVenta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByPrecioVenta() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'precioVenta', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByPrecioVentaDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'precioVenta', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByProductoId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'productoId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByProductoIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'productoId', Sort.desc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.asc);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QAfterSortBy>
  thenByTiendaIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'tiendaId', Sort.desc);
    });
  }
}

extension ProductoCatalogoIsarQueryWhereDistinct
    on QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct> {
  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByCategoriaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'categoriaId');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByCodigoBarras({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'codigoBarras', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByCodigoInterno({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(
        r'codigoInterno',
        caseSensitive: caseSensitive,
      );
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByDescripcionCorta({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(
        r'descripcionCorta',
        caseSensitive: caseSensitive,
      );
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByExistenciaActual({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(
        r'existenciaActual',
        caseSensitive: caseSensitive,
      );
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByImagenUrl({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'imagenUrl', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByNombre({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'nombre', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByPermitirVenta() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'permitirVenta');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByPrecioVenta({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'precioVenta', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByProductoId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'productoId');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, ProductoCatalogoIsar, QDistinct>
  distinctByTiendaId() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'tiendaId');
    });
  }
}

extension ProductoCatalogoIsarQueryProperty
    on
        QueryBuilder<
          ProductoCatalogoIsar,
          ProductoCatalogoIsar,
          QQueryProperty
        > {
  QueryBuilder<ProductoCatalogoIsar, int, QQueryOperations> idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, int?, QQueryOperations>
  categoriaIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'categoriaId');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String?, QQueryOperations>
  codigoBarrasProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'codigoBarras');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String, QQueryOperations>
  codigoInternoProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'codigoInterno');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String?, QQueryOperations>
  descripcionCortaProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'descripcionCorta');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String, QQueryOperations>
  existenciaActualProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'existenciaActual');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String?, QQueryOperations>
  imagenUrlProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'imagenUrl');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String, QQueryOperations>
  nombreProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'nombre');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, bool, QQueryOperations>
  permitirVentaProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'permitirVenta');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, String, QQueryOperations>
  precioVentaProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'precioVenta');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, int, QQueryOperations>
  productoIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'productoId');
    });
  }

  QueryBuilder<ProductoCatalogoIsar, int, QQueryOperations> tiendaIdProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'tiendaId');
    });
  }
}
