# AI Gallery Search

An intelligent Android gallery app that lets you search photos using natural language.  
Built with **Jetpack Compose**, **Room Database**, and **Google ML Kit**.

## ✨ Features
- **AI-Powered Search**: Query photos with phrases like *"dogs at beach"*, *"sunset"*, or *"food"*  
- **Flexible Search Modes**: Exact, Prefix, Contains, and Fuzzy (AI similarity)  
- **Real-time Indexing**: Automatically scans and indexes your gallery  
- **Vector Embeddings**: Semantic embeddings for smarter search  
- **Modern UI**: Material 3 interface with Jetpack Compose  
- **Optimized Performance**: Batch processing, caching, and background indexing  

## 🏗 Architecture
The project follows **Clean Architecture** with three layers:
- **Data Layer**: Room database & repositories  
- **Domain Layer**: Use cases & business logic  
- **Presentation Layer**: MVVM with Jetpack Compose  
- **Dependency Injection**: Koin  

## ⚙️ How It Works

### 1. Image Indexing
On first launch, the app:
- Scans your gallery using **MediaStore API**  
- Analyzes each image with **Google ML Kit** for object/scene detection  
- Generates **128-dimensional embeddings** representing image content  
- Stores metadata (labels, embeddings, file info) in **Room database**  

### 2. Search Process
When searching:
- **Text Matching**: Matches image labels based on selected search mode  
- **Semantic Matching**: Converts query into embeddings and compares similarity  
- **Score Combination**: Text and semantic scores combined (60/40 weight)  
- **Result Ranking**: Returns top results sorted by relevance  

### 3. Search Modes
- **EXACT** → `"dog"` matches only images labeled `"dog"`  
- **PREFIX** → `"hand"` matches `"handbag"`, `"handmade"`  
- **CONTAINS** → `"hand"` matches `"handbag"`, `"secondhand"`  
- **FUZZY** → Smart AI similarity with position weighting  

## ⚡ Performance Considerations

### Indexing
- **Batch Processing**: Images processed in groups of 10  
- **Background Threading**: Heavy operations on `Dispatchers.IO`  
- **Progress Updates**: Real-time updates with Flow  
- **Error Handling**: Continues even if some images fail  

### Search
- **Embedding Cache**: Frequently used vectors cached in memory  
- **Result Limiting**: Top 100 results max  
- **Query Debouncing**: 300ms delay to avoid redundant searches  
- **Similarity Thresholds**: Filters out low-relevance results early  

### Memory Management
- **Image Loading**: Coil handles caching & memory efficiently  
- **Database**: Room with lazy loading and paging support  
- **Embedding Storage**: Optimized byte array storage in SQLite  


