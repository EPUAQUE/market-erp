// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'metadato_local_isar.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetMetadatoLocalIsarCollection on Isar {
  IsarCollection<MetadatoLocalIsar> get metadatoLocalIsars => this.collection();
}

const MetadatoLocalIsarSchema = CollectionSchema(
  name: r'MetadatoLocalIsar',
  id: -864818845074466491,
  properties: {
    r'esquemaVersion': PropertySchema(
      id: 0,
      name: r'esquemaVersion',
      type: IsarType.long,
    ),
  },

  estimateSize: _metadatoLocalIsarEstimateSize,
  serialize: _metadatoLocalIsarSerialize,
  deserialize: _metadatoLocalIsarDeserialize,
  deserializeProp: _metadatoLocalIsarDeserializeProp,
  idName: r'id',
  indexes: {},
  links: {},
  embeddedSchemas: {},

  getId: _metadatoLocalIsarGetId,
  getLinks: _metadatoLocalIsarGetLinks,
  attach: _metadatoLocalIsarAttach,
  version: '3.3.2',
);

int _metadatoLocalIsarEstimateSize(
  MetadatoLocalIsar object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  return bytesCount;
}

void _metadatoLocalIsarSerialize(
  MetadatoLocalIsar object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeLong(offsets[0], object.esquemaVersion);
}

MetadatoLocalIsar _metadatoLocalIsarDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = MetadatoLocalIsar();
  object.esquemaVersion = reader.readLong(offsets[0]);
  object.id = id;
  return object;
}

P _metadatoLocalIsarDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readLong(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _metadatoLocalIsarGetId(MetadatoLocalIsar object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _metadatoLocalIsarGetLinks(
  MetadatoLocalIsar object,
) {
  return [];
}

void _metadatoLocalIsarAttach(
  IsarCollection<dynamic> col,
  Id id,
  MetadatoLocalIsar object,
) {
  object.id = id;
}

extension MetadatoLocalIsarQueryWhereSort
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QWhere> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhere> anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }
}

extension MetadatoLocalIsarQueryWhere
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QWhereClause> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhereClause>
  idEqualTo(Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(lower: id, upper: id));
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhereClause>
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

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhereClause>
  idGreaterThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhereClause>
  idLessThan(Id id, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterWhereClause>
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

extension MetadatoLocalIsarQueryFilter
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QFilterCondition> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
  esquemaVersionEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'esquemaVersion', value: value),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
  esquemaVersionGreaterThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.greaterThan(
          include: include,
          property: r'esquemaVersion',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
  esquemaVersionLessThan(int value, {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.lessThan(
          include: include,
          property: r'esquemaVersion',
          value: value,
        ),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
  esquemaVersionBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.between(
          property: r'esquemaVersion',
          lower: lower,
          includeLower: includeLower,
          upper: upper,
          includeUpper: includeUpper,
        ),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
  idEqualTo(Id value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(
        FilterCondition.equalTo(property: r'id', value: value),
      );
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
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

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
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

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterFilterCondition>
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
}

extension MetadatoLocalIsarQueryObject
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QFilterCondition> {}

extension MetadatoLocalIsarQueryLinks
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QFilterCondition> {}

extension MetadatoLocalIsarQuerySortBy
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QSortBy> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy>
  sortByEsquemaVersion() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'esquemaVersion', Sort.asc);
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy>
  sortByEsquemaVersionDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'esquemaVersion', Sort.desc);
    });
  }
}

extension MetadatoLocalIsarQuerySortThenBy
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QSortThenBy> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy>
  thenByEsquemaVersion() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'esquemaVersion', Sort.asc);
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy>
  thenByEsquemaVersionDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'esquemaVersion', Sort.desc);
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy> thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QAfterSortBy>
  thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }
}

extension MetadatoLocalIsarQueryWhereDistinct
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QDistinct> {
  QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QDistinct>
  distinctByEsquemaVersion() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'esquemaVersion');
    });
  }
}

extension MetadatoLocalIsarQueryProperty
    on QueryBuilder<MetadatoLocalIsar, MetadatoLocalIsar, QQueryProperty> {
  QueryBuilder<MetadatoLocalIsar, int, QQueryOperations> idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<MetadatoLocalIsar, int, QQueryOperations>
  esquemaVersionProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'esquemaVersion');
    });
  }
}
